package edu.seu.vcampus.client.module.library;

import edu.seu.vcampus.common.protocol.ErrorCodes;
import edu.seu.vcampus.common.protocol.Response;

/** Library-facing text for the existing response error codes. */
final class LibraryMessages {
    private LibraryMessages() { }

    static String failure(Response response) {
        return switch (response.getCode()) {
            case ErrorCodes.AUTH_REQUIRED -> "登录已失效，请重新登录";
            case ErrorCodes.AUTH_FORBIDDEN -> "没有执行此操作的权限";
            case ErrorCodes.LIBRARY_BOOK_NOT_FOUND -> "图书不存在，请刷新列表";
            case ErrorCodes.LIBRARY_NO_AVAILABLE_COPY -> "暂无可借库存，请刷新列表";
            case ErrorCodes.LIBRARY_BORROW_LIMIT_REACHED -> "最多同时借阅 5 本，请先归还图书";
            case ErrorCodes.LIBRARY_ALREADY_BORROWED -> "这本书尚未归还，不能重复借阅";
            case ErrorCodes.LIBRARY_OVERDUE_BORROW_EXISTS -> "存在逾期未还图书，请先归还";
            case ErrorCodes.LIBRARY_BORROW_RECORD_NOT_FOUND -> "未找到本人的借阅记录，请刷新列表";
            case ErrorCodes.LIBRARY_ALREADY_RETURNED -> "该记录已归还，请刷新列表";
            case ErrorCodes.COMMON_INVALID_REQUEST, ErrorCodes.COMMON_INVALID_ARGUMENT ->
                    "请求参数不正确，请检查输入或刷新列表";
            case ErrorCodes.COMMON_SERVER_ERROR -> "服务器处理失败，请刷新记录核对状态";
            default -> response.getMessage();
        };
    }
}
