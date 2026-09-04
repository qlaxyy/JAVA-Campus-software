package edu.seu.vcampus.client;

import edu.seu.vcampus.client.application.ClientContext;
import edu.seu.vcampus.client.infrastructure.CampusClient;
import edu.seu.vcampus.common.hospital.DoctorApplicationListResponse;
import edu.seu.vcampus.common.hospital.DoctorApplicationStatus;
import edu.seu.vcampus.common.hospital.DoctorApplicationType;
import edu.seu.vcampus.common.hospital.DoctorApplicationView;
import edu.seu.vcampus.common.hospital.HospitalActions;
import edu.seu.vcampus.common.hospital.HospitalMode;
import edu.seu.vcampus.common.hospital.HospitalModeAccessView;
import edu.seu.vcampus.common.hospital.ReviewDoctorApplicationRequest;
import edu.seu.vcampus.common.hospital.SubmitDoctorApplicationRequest;
import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;
import edu.seu.vcampus.server.infrastructure.CampusServer;
import edu.seu.vcampus.server.infrastructure.database.AccessDatabase;
import edu.seu.vcampus.server.module.ServerModules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoctorOnboardingIntegrationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void approvalReusesExistingAccountAndGrantsDoctorMode() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext hospitalAdministrator = client(server);
            assertTrue(hospitalAdministrator.login(
                    "20260008", password()).isSuccess());
            Response submitted = hospitalAdministrator.send(
                    HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                    existingApplication("20260001"));
            assertTrue(submitted.isSuccess());

            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());
            DoctorApplicationView pending = pendingApplication(administrator);
            Response approved = administrator.send(
                    HospitalActions.REVIEW_DOCTOR_APPLICATION,
                    new ReviewDoctorApplicationRequest(pending.getRequestId(), true));
            assertTrue(approved.isSuccess());
            DoctorApplicationView reviewed = assertInstanceOf(
                    DoctorApplicationView.class, approved.getData());
            assertEquals(DoctorApplicationStatus.APPROVED, reviewed.getStatus());
            assertEquals("U-STUDENT-001", reviewed.getTargetUserId());
            assertEquals("20260001", reviewed.getUsername());

            ClientContext doctor = client(server);
            assertTrue(doctor.login("20260001", password()).isSuccess());
            HospitalModeAccessView access = assertInstanceOf(
                    HospitalModeAccessView.class,
                    doctor.send(HospitalActions.GET_MODE_ACCESS, null).getData());
            assertTrue(access.canAccess(HospitalMode.DOCTOR));
        }
    }

    @Test
    void approvalCreatesMissingAccountWithDevelopmentPassword() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext hospitalAdministrator = client(server);
            assertTrue(hospitalAdministrator.login(
                    "20260008", password()).isSuccess());
            assertTrue(hospitalAdministrator.send(
                    HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                    externalApplication("新医生")).isSuccess());

            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());
            DoctorApplicationView pending = pendingApplication(administrator);
            Response approval = administrator.send(
                    HospitalActions.REVIEW_DOCTOR_APPLICATION,
                    new ReviewDoctorApplicationRequest(pending.getRequestId(), true));
            assertTrue(approval.isSuccess());
            DoctorApplicationView reviewed = assertInstanceOf(
                    DoctorApplicationView.class, approval.getData());
            assertEquals("20260009", reviewed.getUsername());
            assertFalse(reviewed.getUsername().equals("20260001"));

            DoctorApplicationListResponse hospitalHistory = assertInstanceOf(
                    DoctorApplicationListResponse.class,
                    hospitalAdministrator.send(
                            HospitalActions.LIST_DOCTOR_APPLICATIONS, null).getData());
            DoctorApplicationView delivered = hospitalHistory.getApplications().stream()
                    .filter(application -> application.getRequestId()
                            .equals(reviewed.getRequestId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(reviewed.getUsername(), delivered.getUsername());

            ClientContext doctor = client(server);
            assertTrue(doctor.login(reviewed.getUsername(), password()).isSuccess());
            assertNotNull(doctor.currentSession().orElseThrow().getUserId());
            HospitalModeAccessView access = assertInstanceOf(
                    HospitalModeAccessView.class,
                    doctor.send(HospitalActions.GET_MODE_ACCESS, null).getData());
            assertTrue(access.canAccess(HospitalMode.DOCTOR));
        }
    }

    @Test
    void ordinaryAccountCannotSubmitOrReviewDoctorApplication() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext student = client(server);
            assertTrue(student.login("20260001", password()).isSuccess());
            Response submitted = student.send(
                    HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                    externalApplication("无权申请"));
            assertFalse(submitted.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, submitted.getCode());

            Response reviewed = student.send(
                    HospitalActions.REVIEW_DOCTOR_APPLICATION,
                    new ReviewDoctorApplicationRequest("missing", true));
            assertFalse(reviewed.isSuccess());
            assertEquals(ErrorCodes.AUTH_FORBIDDEN, reviewed.getCode());
        }
    }

    @Test
    void approvedAccountAndDoctorProfileSurviveServerRestart() throws Exception {
        Path database = temporaryDirectory.resolve("doctor-onboarding.accdb");
        String generatedUsername;
        try (CampusServer first = new CampusServer(
                0, ServerModules.createPersistentRouter(database))) {
            first.start();
            ClientContext hospitalAdministrator = client(first);
            assertTrue(hospitalAdministrator.login(
                    "20260008", password()).isSuccess());
            assertTrue(hospitalAdministrator.send(
                    HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                    externalApplication("持久化医生")).isSuccess());

            ClientContext administrator = client(first);
            assertTrue(administrator.login("20260003", password()).isSuccess());
            DoctorApplicationView pending = pendingApplication(administrator);
            Response approval = administrator.send(
                    HospitalActions.REVIEW_DOCTOR_APPLICATION,
                    new ReviewDoctorApplicationRequest(pending.getRequestId(), true));
            assertTrue(approval.isSuccess());
            generatedUsername = assertInstanceOf(
                    DoctorApplicationView.class, approval.getData()).getUsername();
        }

        try (CampusServer restarted = new CampusServer(
                0, ServerModules.createPersistentRouter(database))) {
            restarted.start();
            ClientContext doctor = client(restarted);
            assertTrue(doctor.login(generatedUsername, password()).isSuccess());
            HospitalModeAccessView access = assertInstanceOf(
                    HospitalModeAccessView.class,
                    doctor.send(HospitalActions.GET_MODE_ACCESS, null).getData());
            assertTrue(access.canAccess(HospitalMode.DOCTOR));
        }
    }

    @Test
    void externalDoctorNeverSilentlyReusesAnExistingLoginName() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext hospitalAdministrator = client(server);
            assertTrue(hospitalAdministrator.login(
                    "20260008", password()).isSuccess());
            assertTrue(hospitalAdministrator.send(
                    HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                    externalApplication("与学生同名的医生")).isSuccess());

            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());
            DoctorApplicationView pending = pendingApplication(administrator);
            Response approval = administrator.send(
                    HospitalActions.REVIEW_DOCTOR_APPLICATION,
                    new ReviewDoctorApplicationRequest(pending.getRequestId(), true));

            DoctorApplicationView reviewed = assertInstanceOf(
                    DoctorApplicationView.class, approval.getData());
            assertTrue(approval.isSuccess());
            assertFalse(reviewed.getUsername().equals("20260001"));
            assertFalse(reviewed.getTargetUserId().equals("U-STUDENT-001"));
        }
    }

    @Test
    void existingAccountPathRejectsUnknownLoginName() throws Exception {
        try (CampusServer server = new CampusServer(0, 2)) {
            server.start();
            ClientContext hospitalAdministrator = client(server);
            assertTrue(hospitalAdministrator.login(
                    "20260008", password()).isSuccess());

            Response response = hospitalAdministrator.send(
                    HospitalActions.SUBMIT_DOCTOR_APPLICATION,
                    existingApplication("20269999"));

            assertFalse(response.isSuccess());
            assertEquals(ErrorCodes.HOSPITAL_DOCTOR_APPLICATION_CONFLICT, response.getCode());
        }
    }

    @Test
    void legacyHospitalTablesAreMigratedWithoutLosingDoctorBinding() throws Exception {
        Path database = temporaryDirectory.resolve("legacy-hospital.accdb");
        prepareLegacyHospitalTables(database);

        try (CampusServer server = new CampusServer(
                0, ServerModules.createPersistentRouter(database))) {
            server.start();
            ClientContext doctor = client(server);
            assertTrue(doctor.login("20260002", password()).isSuccess());
            HospitalModeAccessView access = assertInstanceOf(
                    HospitalModeAccessView.class,
                    doctor.send(HospitalActions.GET_MODE_ACCESS, null).getData());
            assertTrue(access.canAccess(HospitalMode.DOCTOR));

            ClientContext administrator = client(server);
            assertTrue(administrator.login("20260003", password()).isSuccess());
            DoctorApplicationListResponse applications = assertInstanceOf(
                    DoctorApplicationListResponse.class,
                    administrator.send(
                            HospitalActions.LIST_DOCTOR_APPLICATIONS, null).getData());
            DoctorApplicationView migrated = applications.getApplications().getFirst();
            assertEquals(DoctorApplicationType.EXISTING_ACCOUNT,
                    migrated.getApplicationType());
        }
    }

    private static void prepareLegacyHospitalTables(Path databasePath) throws Exception {
        AccessDatabase database = new AccessDatabase(databasePath);
        try (Connection connection = database.openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE tblHospitalDoctor ("
                    + "doctorUserId TEXT(36) PRIMARY KEY, "
                    + "departmentId TEXT(36) NOT NULL, "
                    + "doctorTitle TEXT(50) NOT NULL, active YESNO NOT NULL)");
            statement.executeUpdate("INSERT INTO tblHospitalDoctor "
                    + "(doctorUserId, departmentId, doctorTitle, active) VALUES "
                    + "('U-TEACHER-001', 'dept-general', '演示医生', TRUE)");
            statement.executeUpdate("CREATE TABLE tblHospitalDoctorApplication ("
                    + "requestId TEXT(40) PRIMARY KEY, username TEXT(50) NOT NULL, "
                    + "displayName TEXT(100) NOT NULL, departmentId TEXT(36) NOT NULL, "
                    + "doctorTitle TEXT(50) NOT NULL, requestedByUserId TEXT(36) NOT NULL, "
                    + "applicationStatus TEXT(20) NOT NULL, targetUserId TEXT(36), "
                    + "reviewedByUserId TEXT(36), createdAt DATETIME NOT NULL)");
            statement.executeUpdate("INSERT INTO tblHospitalDoctorApplication "
                    + "(requestId, username, displayName, departmentId, doctorTitle, "
                    + "requestedByUserId, applicationStatus, targetUserId, "
                    + "reviewedByUserId, createdAt) VALUES "
                    + "('DAR-LEGACY-001', 'student001', '演示学生', 'dept-general', "
                    + "'主治医师', 'U-HOSPITAL-ADMIN-001', 'PENDING', NULL, NULL, NOW())");
        }
    }

    private static DoctorApplicationView pendingApplication(ClientContext administrator)
            throws Exception {
        Response listed = administrator.send(
                HospitalActions.LIST_DOCTOR_APPLICATIONS, null);
        assertTrue(listed.isSuccess());
        DoctorApplicationListResponse applications = assertInstanceOf(
                DoctorApplicationListResponse.class, listed.getData());
        return applications.getApplications().stream()
                .filter(application -> application.getStatus()
                        == DoctorApplicationStatus.PENDING)
                .findFirst()
                .orElseThrow();
    }

    private static SubmitDoctorApplicationRequest existingApplication(String username) {
        return SubmitDoctorApplicationRequest.forExistingAccount(
                username, "dept-general", "主治医师");
    }

    private static SubmitDoctorApplicationRequest externalApplication(String displayName) {
        return SubmitDoctorApplicationRequest.forExternalDoctor(
                displayName, "dept-general", "主治医师");
    }

    private static ClientContext client(CampusServer server) {
        return new ClientContext(new CampusClient("127.0.0.1", server.getPort()));
    }

    private static char[] password() {
        return "123456".toCharArray();
    }
}
