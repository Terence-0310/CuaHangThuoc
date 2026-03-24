package common;

import domain.repository.*;
import infrastructure.repository.*;
import service.*;
import service.impl.*;

/**
 * ServiceFactory: DI Container đơn giản (Dependency Inversion Principle)
 * 
 * Tất cả Service nhận Interface qua constructor, KHÔNG new trực tiếp Impl.
 * GUI chỉ gọi ServiceFactory.getXxxService() → nhận Interface.
 */
public class ServiceFactory {

    // ===== Repository Singletons (Infrastructure layer) =====
    private static final IUserRepository userRepo = new UserRepositoryImpl();
    private static final IProductRepository productRepo = new ProductRepositoryImpl();
    private static final IBatchRepository batchRepo = new BatchRepositoryImpl();
    private static final ICustomerRepository customerRepo = new CustomerRepositoryImpl();
    private static final IInvoiceRepository invoiceRepo = new InvoiceRepositoryImpl();
    private static final IInvoiceDetailRepository invoiceDetailRepo = new InvoiceDetailRepositoryImpl();
    private static final IReportRepository reportRepo = new ReportRepositoryImpl();
    private static final ISupplierRepository supplierRepo = new SupplierRepositoryImpl();

    // ===== Service Getters — Inject repos qua constructor =====

    public static IAuthService getAuthService() {
        return new AuthServiceImpl(userRepo);
    }

    public static IProductService getProductService() {
        return new ProductServiceImpl(productRepo);
    }

    public static IBatchService getBatchService() {
        return new BatchServiceImpl(batchRepo, productRepo);
    }

    public static ICustomerService getCustomerService() {
        return new CustomerServiceImpl(customerRepo);
    }

    public static ISaleService getSaleService() {
        return new SaleServiceImpl(invoiceRepo, invoiceDetailRepo, batchRepo, customerRepo);
    }

    public static IInvoiceService getInvoiceService() {
        return new InvoiceServiceImpl(invoiceRepo, invoiceDetailRepo);
    }

    public static IReportService getReportService() {
        return new ReportServiceImpl(reportRepo);
    }

    public static ISupplierService getSupplierService() {
        return new SupplierServiceImpl(supplierRepo);
    }
}
