package edu.seu.vcampus.client.module;

import edu.seu.vcampus.client.module.course.CourseClientModule;
import edu.seu.vcampus.client.module.hospital.HospitalClientModule;
import edu.seu.vcampus.client.module.library.LibraryClientModule;
import edu.seu.vcampus.client.module.shop.ShopClientModule;
import edu.seu.vcampus.client.module.student.StudentClientModule;
import edu.seu.vcampus.client.module.user.UserClientModule;

import java.util.List;

/**
 * Fixed catalog of the six agreed client-side business modules.
 */
public final class ClientModules {

    private ClientModules() {
    }

    /**
     * Returns modules in the agreed navigation order.
     *
     * @return immutable module list
     */
    public static List<ClientModule> all() {
        return List.of(
                new UserClientModule(),
                new StudentClientModule(),
                new CourseClientModule(),
                new LibraryClientModule(),
                new ShopClientModule(),
                new HospitalClientModule());
    }
}
