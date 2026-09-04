package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.DepartmentListResponse;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.SearchSlotsRequest;
import edu.seu.vcampus.common.hospital.SlotListResponse;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HospitalSearchIntegrationTest {

    @Test
    void serverCalculatesPatientDoctorAndAdminModeAccess() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            assertTrue(context.login(
                    "20260002", "123456".toCharArray()).isSuccess());
            Response doctorResponse = context.send(HospitalActions.GET_MODE_ACCESS, null);
            HospitalModeAccessView doctorAccess = assertInstanceOf(
                    HospitalModeAccessView.class, doctorResponse.getData());
            assertTrue(doctorAccess.canAccess(HospitalMode.PATIENT));
            assertTrue(doctorAccess.canAccess(HospitalMode.DOCTOR));
            assertFalse(doctorAccess.canAccess(HospitalMode.ADMIN));

            assertTrue(context.logout().isSuccess());
            assertTrue(context.login("20260008", "123456".toCharArray()).isSuccess());
            Response adminResponse = context.send(HospitalActions.GET_MODE_ACCESS, null);
            HospitalModeAccessView adminAccess = assertInstanceOf(
                    HospitalModeAccessView.class, adminResponse.getData());
            assertTrue(adminAccess.canAccess(HospitalMode.PATIENT));
            assertFalse(adminAccess.canAccess(HospitalMode.DOCTOR));
            assertTrue(adminAccess.canAccess(HospitalMode.ADMIN));
        }
    }

    @Test
    void loggedInStudentCanSearchHospitalSlotsThroughSocket() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext context = new ClientContext(
                    new CampusClient("127.0.0.1", server.getPort()));

            Response beforeLogin = context.send(HospitalActions.LIST_DEPARTMENTS, null);
            assertFalse(beforeLogin.isSuccess());
            assertEquals(ErrorCodes.AUTH_REQUIRED, beforeLogin.getCode());

            assertTrue(context.login("20260001", "123456".toCharArray()).isSuccess());

            Response departmentsResponse = context.send(
                    HospitalActions.LIST_DEPARTMENTS, null);
            assertTrue(departmentsResponse.isSuccess());
            DepartmentListResponse departments = assertInstanceOf(
                    DepartmentListResponse.class, departmentsResponse.getData());
            assertEquals(4, departments.getDepartments().size());

            Response slotsResponse = context.send(
                    HospitalActions.SEARCH_SLOTS,
                    SearchSlotsRequest.firstVisit("dept-psychology", null));
            assertTrue(slotsResponse.isSuccess());
            SlotListResponse slots = assertInstanceOf(
                    SlotListResponse.class, slotsResponse.getData());
            assertFalse(slots.getSlots().isEmpty());

            Response invalidDepartment = context.send(
                    HospitalActions.SEARCH_SLOTS,
                    SearchSlotsRequest.firstVisit("dept-missing", null));
            assertFalse(invalidDepartment.isSuccess());
            assertEquals(ErrorCodes.COMMON_INVALID_REQUEST, invalidDepartment.getCode());
        }
    }
}
