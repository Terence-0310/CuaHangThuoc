package service.impl;

import domain.entity.Invoice;
import domain.entity.InvoiceDetail;
import domain.repository.IInvoiceDetailRepository;
import domain.repository.IInvoiceRepository;
import service.IInvoiceService;
import java.util.List;

/**
 * Service Impl: Xem hóa đơn & lịch sử (ISP — tách khỏi SaleService)
 */
public class InvoiceServiceImpl implements IInvoiceService {

    private final IInvoiceRepository invoiceRepo;
    private final IInvoiceDetailRepository detailRepo;

    public InvoiceServiceImpl(IInvoiceRepository invoiceRepo, IInvoiceDetailRepository detailRepo) {
        this.invoiceRepo = invoiceRepo;
        this.detailRepo = detailRepo;
    }

    @Override
    public List<Invoice> getByCustomerId(int maKH) {
        return invoiceRepo.getByCustomerId(maKH);
    }

    @Override
    public List<InvoiceDetail> getDetails(int maHD) {
        return detailRepo.getByInvoiceId(maHD);
    }

    @Override
    public List<Invoice> getAll() {
        return invoiceRepo.getAll();
    }
}
