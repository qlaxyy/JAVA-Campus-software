package edu.seu.vcampus.common.protocol;

/**
 * Error codes that are safe to send to clients.
 */
public final class ErrorCodes {

    public static final String SUCCESS = "SUCCESS";
    public static final String COMMON_INVALID_REQUEST = "COMMON_INVALID_REQUEST";
    public static final String COMMON_UNKNOWN_ACTION = "COMMON_UNKNOWN_ACTION";
    public static final String COMMON_SERVER_ERROR = "COMMON_SERVER_ERROR";
    public static final String COMMON_INVALID_ARGUMENT = "COMMON_INVALID_ARGUMENT";
    public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
    public static final String AUTH_REQUIRED = "AUTH_REQUIRED";
    public static final String AUTH_FORBIDDEN = "AUTH_FORBIDDEN";
    public static final String USER_ACCOUNT_NOT_FOUND = "USER_ACCOUNT_NOT_FOUND";
    public static final String USER_USERNAME_EXISTS = "USER_USERNAME_EXISTS";
    public static final String USER_SELF_DISABLE_FORBIDDEN = "USER_SELF_DISABLE_FORBIDDEN";
    public static final String LIBRARY_BOOK_NOT_FOUND = "LIBRARY_BOOK_NOT_FOUND";
    public static final String LIBRARY_NO_AVAILABLE_COPY = "LIBRARY_NO_AVAILABLE_COPY";
    public static final String LIBRARY_BORROW_LIMIT_REACHED = "LIBRARY_BORROW_LIMIT_REACHED";
    public static final String LIBRARY_ALREADY_BORROWED = "LIBRARY_ALREADY_BORROWED";
    public static final String LIBRARY_OVERDUE_BORROW_EXISTS =
            "LIBRARY_OVERDUE_BORROW_EXISTS";
    public static final String HOSPITAL_DOCTOR_APPLICATION_NOT_FOUND =
            "HOSPITAL_DOCTOR_APPLICATION_NOT_FOUND";
    public static final String HOSPITAL_DOCTOR_APPLICATION_CONFLICT =
            "HOSPITAL_DOCTOR_APPLICATION_CONFLICT";
    public static final String HOSPITAL_SCHEDULE_NOT_FOUND =
            "HOSPITAL_SCHEDULE_NOT_FOUND";
    public static final String HOSPITAL_SCHEDULE_CLOSED =
            "HOSPITAL_SCHEDULE_CLOSED";
    public static final String HOSPITAL_SCHEDULE_STARTED =
            "HOSPITAL_SCHEDULE_STARTED";
    public static final String HOSPITAL_SCHEDULE_CONFLICT =
            "HOSPITAL_SCHEDULE_CONFLICT";
    public static final String HOSPITAL_SCHEDULE_HAS_APPOINTMENTS =
            "HOSPITAL_SCHEDULE_HAS_APPOINTMENTS";
    public static final String HOSPITAL_BILL_NOT_FOUND =
            "HOSPITAL_BILL_NOT_FOUND";
    public static final String HOSPITAL_BILL_NOT_PAYABLE =
            "HOSPITAL_BILL_NOT_PAYABLE";
    public static final String HOSPITAL_SLOT_FULL = "HOSPITAL_SLOT_FULL";
    public static final String HOSPITAL_DUPLICATE_APPOINTMENT =
            "HOSPITAL_DUPLICATE_APPOINTMENT";
    public static final String HOSPITAL_SELF_BOOKING_FORBIDDEN =
            "HOSPITAL_SELF_BOOKING_FORBIDDEN";
    public static final String HOSPITAL_APPOINTMENT_NOT_FOUND =
            "HOSPITAL_APPOINTMENT_NOT_FOUND";
    public static final String HOSPITAL_APPOINTMENT_NOT_CANCELLABLE =
            "HOSPITAL_APPOINTMENT_NOT_CANCELLABLE";
    public static final String HOSPITAL_APPOINTMENT_STARTED =
            "HOSPITAL_APPOINTMENT_STARTED";
    public static final String HOSPITAL_FIRST_VISIT_WITH_SOURCE =
            "HOSPITAL_FIRST_VISIT_WITH_SOURCE";
    public static final String HOSPITAL_FOLLOW_UP_WITHOUT_SOURCE =
            "HOSPITAL_FOLLOW_UP_WITHOUT_SOURCE";
    public static final String HOSPITAL_FOLLOW_UP_SOURCE_INVALID =
            "HOSPITAL_FOLLOW_UP_SOURCE_INVALID";
    public static final String HOSPITAL_FOLLOW_UP_DOCTOR_MISMATCH =
            "HOSPITAL_FOLLOW_UP_DOCTOR_MISMATCH";
    public static final String HOSPITAL_FOLLOW_UP_WINDOW_EXPIRED =
            "HOSPITAL_FOLLOW_UP_WINDOW_EXPIRED";
    public static final String HOSPITAL_BOOKING_CONFLICT =
            "HOSPITAL_BOOKING_CONFLICT";
    public static final String HOSPITAL_APPOINTMENT_NOT_CONSULTABLE =
            "HOSPITAL_APPOINTMENT_NOT_CONSULTABLE";
    public static final String HOSPITAL_APPOINTMENT_NOT_NO_SHOW =
            "HOSPITAL_APPOINTMENT_NOT_NO_SHOW";
    public static final String HOSPITAL_CONSULTATION_ALREADY_EXISTS =
            "HOSPITAL_CONSULTATION_ALREADY_EXISTS";
    public static final String HOSPITAL_EXAMINATION_NOT_FOUND =
            "HOSPITAL_EXAMINATION_NOT_FOUND";
    public static final String HOSPITAL_EXAMINATION_STATE_INVALID =
            "HOSPITAL_EXAMINATION_STATE_INVALID";
    public static final String HOSPITAL_RESULT_REVIEW_ALREADY_BOOKED =
            "HOSPITAL_RESULT_REVIEW_ALREADY_BOOKED";
    public static final String HOSPITAL_DEPARTMENT_NOT_FOUND =
            "HOSPITAL_DEPARTMENT_NOT_FOUND";
    public static final String HOSPITAL_DEPARTMENT_CONFLICT =
            "HOSPITAL_DEPARTMENT_CONFLICT";
    public static final String HOSPITAL_DEPARTMENT_IN_USE =
            "HOSPITAL_DEPARTMENT_IN_USE";
    public static final String CARD_INSUFFICIENT_BALANCE = "CARD_INSUFFICIENT_BALANCE";
    public static final String SHOP_INSUFFICIENT_BALANCE = "SHOP_INSUFFICIENT_BALANCE";
    public static final String SHOP_OUT_OF_STOCK = "SHOP_OUT_OF_STOCK";
    public static final String SHOP_PRODUCT_NOT_FOUND = "SHOP_PRODUCT_NOT_FOUND";
    public static final String SHOP_CATEGORY_NOT_FOUND = "SHOP_CATEGORY_NOT_FOUND";
    public static final String SHOP_CATEGORY_EXISTS = "SHOP_CATEGORY_EXISTS";
    public static final String SHOP_ORDER_NOT_FOUND = "SHOP_ORDER_NOT_FOUND";
    public static final String SHOP_ORDER_NOT_CANCELLABLE = "SHOP_ORDER_NOT_CANCELLABLE";
    public static final String LIBRARY_BORROW_RECORD_NOT_FOUND =
            "LIBRARY_BORROW_RECORD_NOT_FOUND";
    public static final String LIBRARY_ALREADY_RETURNED = "LIBRARY_ALREADY_RETURNED";
    public static final String LIBRARY_INVALID_STOCK = "LIBRARY_INVALID_STOCK";
    public static final String LIBRARY_DUPLICATE_ISBN = "LIBRARY_DUPLICATE_ISBN";
    public static final String LIBRARY_CATEGORY_NOT_FOUND = "LIBRARY_CATEGORY_NOT_FOUND";
    public static final String LIBRARY_COPY_NOT_FOUND = "LIBRARY_COPY_NOT_FOUND";
    public static final String LIBRARY_COPY_NOT_AVAILABLE = "LIBRARY_COPY_NOT_AVAILABLE";
    public static final String LIBRARY_INVALID_COPY_STATUS = "LIBRARY_INVALID_COPY_STATUS";
    public static final String LIBRARY_DUPLICATE_BARCODE = "LIBRARY_DUPLICATE_BARCODE";
    public static final String LIBRARY_INVALID_BOOK_STATUS = "LIBRARY_INVALID_BOOK_STATUS";
    public static final String LIBRARY_RESERVATION_NOT_FOUND =
            "LIBRARY_RESERVATION_NOT_FOUND";
    public static final String LIBRARY_DUPLICATE_RESERVATION =
            "LIBRARY_DUPLICATE_RESERVATION";
    public static final String LIBRARY_RESERVATION_LIMIT_REACHED =
            "LIBRARY_RESERVATION_LIMIT_REACHED";
    public static final String LIBRARY_RESERVATION_COOLDOWN =
            "LIBRARY_RESERVATION_COOLDOWN";
    public static final String LIBRARY_RESERVATION_NOT_CANCELLABLE =
            "LIBRARY_RESERVATION_NOT_CANCELLABLE";
    public static final String LIBRARY_INVALID_PICKUP_LOCATION =
            "LIBRARY_INVALID_PICKUP_LOCATION";
    public static final String LIBRARY_COPY_RESERVED_FOR_OTHER =
            "LIBRARY_COPY_RESERVED_FOR_OTHER";

    private ErrorCodes() {
    }
}
