package com.billing.service;

import com.billing.model.BusinessProfile;
import com.billing.model.Order;
import com.billing.model.OrderItem;
import com.billing.repository.BusinessProfileRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class PdfService {

    private final BusinessProfileRepository businessProfileRepository;

    public PdfService(BusinessProfileRepository businessProfileRepository) {
        this.businessProfileRepository = businessProfileRepository;
    }

    public byte[] generateInvoicePdf(Order order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            Optional<BusinessProfile> bizOpt = businessProfileRepository.findAll().stream().findFirst();
            String businessName = bizOpt.map(BusinessProfile::getBusinessName).orElse("BillingSystem Pro");
            String address = bizOpt.map(BusinessProfile::getAddressLine1).orElse("No Address Provided");
            String gstin = bizOpt.map(BusinessProfile::getGstin).orElse("");
            String phone = bizOpt.map(BusinessProfile::getPhone).orElse("");

            // Header Section
            document.add(new Paragraph(businessName).setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph(address).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            if (!phone.isBlank()) document.add(new Paragraph("Phone: " + phone).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            if (!gstin.isBlank()) document.add(new Paragraph("GSTIN: " + gstin).setFontSize(10).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));

            // Invoice Info
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
            infoTable.addCell(new Cell().add(new Paragraph("Invoice #: " + order.getInvoiceNumber())).setBorder(null));
            infoTable.addCell(new Cell().add(new Paragraph("Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")))).setTextAlignment(TextAlignment.RIGHT).setBorder(null));
            infoTable.addCell(new Cell().add(new Paragraph("Customer: " + (order.getCustomer() != null ? order.getCustomer().getName() : ""))).setBorder(null));
            infoTable.addCell(new Cell().add(new Paragraph("Payment: " + order.getPaymentMethod() + " (" + order.getPaymentStatus() + ")")).setTextAlignment(TextAlignment.RIGHT).setBorder(null));
            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // Items Table
            Table table = new Table(UnitValue.createPercentArray(new float[]{40, 20, 20, 20})).useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("Product").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("Price").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.RIGHT));
            table.addHeaderCell(new Cell().add(new Paragraph("Total").setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY).setTextAlignment(TextAlignment.RIGHT));

            for (OrderItem item : order.getItems()) {
                table.addCell(new Cell().add(new Paragraph(item.getProductName())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity()))).setTextAlignment(TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", item.getUnitPrice()))).setTextAlignment(TextAlignment.RIGHT));
                table.addCell(new Cell().add(new Paragraph(String.format("%.2f", item.getLineTotal()))).setTextAlignment(TextAlignment.RIGHT));
            }
            document.add(table);
            document.add(new Paragraph("\n"));

            // Summary
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
            summaryTable.addCell(new Cell().add(new Paragraph("Grand Total:").setBold()).setTextAlignment(TextAlignment.RIGHT).setBorder(null));
            summaryTable.addCell(new Cell().add(new Paragraph(String.format("%.2f", order.getTotalAmount())).setBold()).setTextAlignment(TextAlignment.RIGHT).setBorder(null));
            document.add(summaryTable);

            document.add(new Paragraph("\nThank you for your business!").setTextAlignment(TextAlignment.CENTER).setFontSize(10).setItalic());

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }
}
