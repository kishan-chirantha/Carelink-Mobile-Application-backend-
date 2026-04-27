package lk.kishan.carelink.service;

import lk.kishan.carelink.model.Order;
import lk.kishan.carelink.model.OrderItem;
import lk.kishan.carelink.model.Prescription;
import lk.kishan.carelink.repository.OrderRepository;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceService {

    @Autowired
    private OrderRepository orderRepository;

    private static final BaseColor TEAL      = new BaseColor(0,   106,  96);
    private static final BaseColor TEAL_LITE = new BaseColor(200, 223, 255);
    private static final BaseColor ROW_ALT   = new BaseColor(240, 246, 255);
    private static final BaseColor ROW_DIV   = new BaseColor(221, 238, 255);
    private static final BaseColor PAID_BG   = new BaseColor(227, 242, 253);
    private static final BaseColor BLUE_LINE = new BaseColor(26,  115, 232);
    private static final BaseColor DARK      = new BaseColor(45,   45,  45);
    private static final BaseColor GREY      = new BaseColor(102, 102, 102);
    private static final BaseColor WHITE     = BaseColor.WHITE;

    private static final Font fPharmacy  = f(16, Font.BOLD,   WHITE);
    private static final Font fHdrSub    = f( 8, Font.NORMAL, TEAL_LITE);
    private static final Font fInvoice   = f(24, Font.BOLD,   WHITE);
    private static final Font fTracking  = f(10, Font.BOLD,   TEAL_LITE);
    private static final Font fBillLbl   = f( 8, Font.BOLD,   TEAL);
    private static final Font fCustName  = f(10, Font.BOLD,   DARK);
    private static final Font fCustAddr  = f( 8, Font.NORMAL, GREY);
    private static final Font fMetaLbl   = f( 8, Font.BOLD,   TEAL);
    private static final Font fMetaVal   = f( 8, Font.NORMAL, DARK);
    private static final Font fPaid      = f( 8, Font.BOLD,   TEAL);
    private static final Font fColHdr    = f( 8, Font.BOLD,   WHITE);
    private static final Font fCellNorm  = f( 8, Font.NORMAL, DARK);
    private static final Font fCellBold  = f( 8, Font.BOLD,   DARK);
    private static final Font fSumLbl    = f( 9, Font.NORMAL, GREY);
    private static final Font fSumVal    = f( 9, Font.NORMAL, DARK);
    private static final Font fTotalLbl  = f(11, Font.BOLD,   WHITE);
    private static final Font fTotalRs   = f( 9, Font.BOLD,   TEAL_LITE);
    private static final Font fTotalVal  = f(12, Font.BOLD,   WHITE);
    private static final Font fFooter    = f( 8, Font.BOLD,   TEAL);

    private static Font f(int size, int style, BaseColor color) {
        return new Font(Font.FontFamily.HELVETICA, size, style, color);
    }

    public byte[] generateInvoicePdf(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        syncFirebaseDataAndFilter(order);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 30, 30, 20, 20);
        PdfWriter.getInstance(doc, baos);
        doc.open();

        addHeader(doc, order);
        addSepLine(doc, TEAL, 1.5f, 0f);
        addBillToMeta(doc, order);
        doc.add(Chunk.NEWLINE);
        addColumnHeader(doc);
        addDetailRows(doc, order);
        addSummary(doc, order);
        addFooter(doc);

        doc.close();
        return baos.toByteArray();
    }

    private void syncFirebaseDataAndFilter(Order order) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String firebaseUrl = "https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app/Orders/" + order.getId() + ".json";
            String jsonResponse = restTemplate.getForObject(firebaseUrl, String.class);

            if (jsonResponse != null && !jsonResponse.equals("null")) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode orderNode = mapper.readTree(jsonResponse);

                if (order.getOrderItems() != null && orderNode.has("orderItems")) {
                    JsonNode itemsNode = orderNode.get("orderItems");
                    List<OrderItem> validItems = new ArrayList<>();

                    for (OrderItem item : order.getOrderItems()) {
                        if (item.getProduct() == null) continue;
                        boolean keep = true;

                        for (JsonNode node : itemsNode) {
                            if (node.has("product") && node.get("product").has("name")) {
                                if (node.get("product").get("name").asText().equals(item.getProduct().getName())) {
                                    if (node.has("status") && "OUT_OF_STOCK".equalsIgnoreCase(node.get("status").asText())) {
                                        keep = false;
                                    }
                                    break;
                                }
                            }
                        }
                        if (keep) validItems.add(item);
                    }
                    order.setOrderItems(validItems);
                }

                if (order.getPrescriptions() != null && orderNode.has("prescriptions")) {
                    JsonNode presNode = orderNode.get("prescriptions");
                    List<Prescription> validPrescriptions = new ArrayList<>();

                    for (Prescription p : order.getPrescriptions()) {
                        boolean keep = true;

                        for (JsonNode node : presNode) {
                            if (node.has("imageUrl") && node.get("imageUrl").asText().equals(p.getImageUrl())) {
                                if (node.has("price")) {
                                    p.setPrice(node.get("price").asDouble());
                                }
                                if (node.has("status") && "REJECTED".equalsIgnoreCase(node.get("status").asText())) {
                                    keep = false;
                                }
                                break;
                            }
                        }
                        if (keep) validPrescriptions.add(p);
                    }
                    order.setPrescriptions(validPrescriptions);
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Firebase Sync Failed - " + e.getMessage());
        }
    }

    private void addHeader(Document doc, Order order) throws DocumentException {
        PdfPTable tbl = new PdfPTable(2);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{60f, 40f});

        PdfPCell lc = new PdfPCell();
        lc.setBackgroundColor(TEAL);
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setFixedHeight(90f);
        lc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        lc.setPaddingLeft(10f);
        lc.setPaddingTop(12f);
        lc.setPaddingBottom(12f);

        lc.addElement(para("CareLink Pharmacy", fPharmacy, Element.ALIGN_LEFT));
        lc.addElement(para("No. 123, Galle Road, Colombo 03, Sri Lanka.", fHdrSub, Element.ALIGN_LEFT));
        lc.addElement(para("+94 11 234 5678  |  info@carelink.lk",         fHdrSub, Element.ALIGN_LEFT));
        tbl.addCell(lc);

        PdfPCell rc = new PdfPCell();
        rc.setBackgroundColor(TEAL);
        rc.setBorder(Rectangle.NO_BORDER);
        rc.setFixedHeight(90f);
        rc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        rc.setPaddingRight(12f);
        rc.setPaddingTop(10f);
        rc.setPaddingBottom(10f);

        rc.addElement(para("INVOICE", fInvoice, Element.ALIGN_RIGHT));

        String tid = order.getTrackingId() != null ? order.getTrackingId() : "";
        rc.addElement(para(tid, fTracking, Element.ALIGN_RIGHT));
        tbl.addCell(rc);

        doc.add(tbl);
    }

    private void addBillToMeta(Document doc, Order order) throws DocumentException {
        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.setWidths(new float[]{55f, 45f});
        outer.setSpacingBefore(8f);

        PdfPCell billCell = new PdfPCell();
        billCell.setBorder(Rectangle.NO_BORDER);
        billCell.setPaddingTop(4f);
        billCell.setPaddingLeft(2f);
        billCell.setPaddingBottom(6f);

        billCell.addElement(para("BILL TO", fBillLbl, Element.ALIGN_LEFT));

        String name = nvl(order.getCustomer().getName());
        Paragraph namePara = new Paragraph(name, fCustName);
        namePara.setSpacingBefore(3f);
        billCell.addElement(namePara);

        String addr = nvl(order.getDeliveryAddress());
        Paragraph addrPara = new Paragraph(addr, fCustAddr);
        addrPara.setSpacingBefore(2f);
        billCell.addElement(addrPara);

        outer.addCell(billCell);

        PdfPTable metaTbl = new PdfPTable(2);
        metaTbl.setWidthPercentage(100);
        metaTbl.setWidths(new float[]{55f, 45f});

        metaRow(metaTbl, "INVOICE DATE",
                order.getOrderDate() != null ? order.getOrderDate().toString() : "");

        metaRow(metaTbl, "PAYMENT", nvl(order.getPaymentMethod()));

        PdfPCell emptyLbl = noBorder(new Phrase(""));
        emptyLbl.setPaddingTop(4f);
        metaTbl.addCell(emptyLbl);

        String paymentMethodBadge = "PAID";
        if (order.getPaymentMethod() != null && order.getPaymentMethod().toUpperCase().contains("COD")) {
            paymentMethodBadge = "NOT-PAID";
        }

        PdfPCell paidCell = new PdfPCell(new Phrase(paymentMethodBadge, fPaid));
        paidCell.setBackgroundColor(paymentMethodBadge.equals("NOT-PAID") ? new BaseColor(255, 235, 238) : PAID_BG);
        paidCell.setBorder(Rectangle.NO_BORDER);
        paidCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        paidCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        paidCell.setFixedHeight(18f);
        paidCell.setPaddingTop(2f);
        paidCell.setPaddingBottom(2f);
        metaTbl.addCell(paidCell);

        PdfPCell metaWrapper = new PdfPCell(metaTbl);
        metaWrapper.setBorder(Rectangle.NO_BORDER);
        metaWrapper.setPaddingTop(4f);
        outer.addCell(metaWrapper);

        doc.add(outer);
    }

    private void metaRow(PdfPTable tbl, String label, String value) {
        PdfPCell lc = noBorder(new Phrase(label, fMetaLbl));
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setPaddingBottom(5f);
        tbl.addCell(lc);

        PdfPCell vc = noBorder(new Phrase(value, fMetaVal));
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPaddingBottom(5f);
        tbl.addCell(vc);
    }

    private void addColumnHeader(Document doc) throws DocumentException {
        PdfPTable tbl = new PdfPTable(4);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{45f, 13f, 21f, 21f});

        String[] labels = {"PRODUCT NAME", "QTY", "UNIT PRICE (Rs.)", "AMOUNT (Rs.)"};
        int[]    aligns = {Element.ALIGN_LEFT, Element.ALIGN_CENTER,
                Element.ALIGN_RIGHT, Element.ALIGN_RIGHT};

        for (int i = 0; i < labels.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(labels[i], fColHdr));
            c.setBackgroundColor(TEAL);
            c.setBorder(Rectangle.NO_BORDER);
            c.setFixedHeight(26f);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            c.setHorizontalAlignment(aligns[i]);
            c.setPaddingLeft(i == 0 ? 8f : 2f);
            c.setPaddingRight(i == labels.length - 1 ? 6f : 2f);
            tbl.addCell(c);
        }
        doc.add(tbl);
    }

    private void addDetailRows(Document doc, Order order) throws DocumentException {
        boolean hasItems = (order.getOrderItems() != null && !order.getOrderItems().isEmpty());
        boolean hasPrescriptions = (order.getPrescriptions() != null && !order.getPrescriptions().isEmpty());

        if (!hasItems && !hasPrescriptions) return;

        PdfPTable tbl = new PdfPTable(4);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{45f, 13f, 21f, 21f});

        int idx = 0;

        if (hasItems) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getProduct() == null) continue;

                BaseColor bg    = (idx % 2 == 0) ? ROW_ALT : WHITE;
                String    name  = nvl(item.getProduct().getName(), "Product Item");
                double    price = item.getProduct().getPrice();
                int       qty   = item.getQuantity();
                double    total = price * qty;

                dCell(tbl, name,                          bg, Element.ALIGN_LEFT,   fCellNorm, 8f, 2f);
                dCell(tbl, String.valueOf(qty),           bg, Element.ALIGN_CENTER,  fCellNorm, 2f, 2f);
                dCell(tbl, String.format("%,.2f", price), bg, Element.ALIGN_RIGHT,   fCellNorm, 2f, 2f);
                dCell(tbl, String.format("%,.2f", total), bg, Element.ALIGN_RIGHT,   fCellBold, 2f, 6f);
                idx++;
            }
        }

        if (hasPrescriptions) {
            for (Prescription p : order.getPrescriptions()) {
                BaseColor bg    = (idx % 2 == 0) ? ROW_ALT : WHITE;
                String    name  = "Prescription (ID: " + p.getId() + ")";

                double    price = p.getPrice() != null ? p.getPrice() : 0.0;
                int       qty   = 1;
                double    total = price * qty;

                dCell(tbl, name,                          bg, Element.ALIGN_LEFT,   fCellNorm, 8f, 2f);
                dCell(tbl, String.valueOf(qty),           bg, Element.ALIGN_CENTER,  fCellNorm, 2f, 2f);
                dCell(tbl, String.format("%,.2f", price), bg, Element.ALIGN_RIGHT,   fCellNorm, 2f, 2f);
                dCell(tbl, String.format("%,.2f", total), bg, Element.ALIGN_RIGHT,   fCellBold, 2f, 6f);
                idx++;
            }
        }

        doc.add(tbl);
    }

    private void dCell(PdfPTable tbl, String text, BaseColor bg,
                       int align, Font font, float pl, float pr) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setFixedHeight(24f);
        c.setPaddingLeft(pl);
        c.setPaddingRight(pr);
        c.setBorderWidthBottom(0.5f);
        c.setBorderColorBottom(ROW_DIV);
        c.setBorderWidthTop(0f);
        c.setBorderWidthLeft(0f);
        c.setBorderWidthRight(0f);
        tbl.addCell(c);
    }

    private void addSummary(Document doc, Order order) throws DocumentException {
        addSepLine(doc, TEAL, 1.5f, 5f);

        double sub      = getItemsTotal(order);
        double delivery = order.getDeliveryFee()  != null ? order.getDeliveryFee()  : 450.00;
        double total    = sub + delivery;

        PdfPTable tbl = new PdfPTable(4);
        tbl.setWidthPercentage(100);
        tbl.setWidths(new float[]{32f, 15f, 29f, 24f});
        tbl.setSpacingBefore(10f);

        sumRow(tbl, "Sub Total :",   String.format("Rs.  %,.2f", sub));
        sumRow(tbl, "Delivery Fee :", String.format("Rs.  %,.2f", delivery));

        span2Blank(tbl);
        PdfPCell blueSep = noBorder(new Phrase(""));
        blueSep.setColspan(2);
        blueSep.setFixedHeight(1f);
        blueSep.setBackgroundColor(BLUE_LINE);
        tbl.addCell(blueSep);

        span2Blank(tbl);

        PdfPTable totalInner = new PdfPTable(3);
        totalInner.setWidths(new float[]{35f, 18f, 47f});

        PdfPCell tLbl = noBorder(new Phrase("TOTAL", fTotalLbl));
        tLbl.setBackgroundColor(TEAL);
        tLbl.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tLbl.setPaddingLeft(6f);
        totalInner.addCell(tLbl);

        PdfPCell tRs = noBorder(new Phrase("Rs.", fTotalRs));
        tRs.setBackgroundColor(TEAL);
        tRs.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tRs.setHorizontalAlignment(Element.ALIGN_CENTER);
        totalInner.addCell(tRs);

        PdfPCell tVal = noBorder(new Phrase(String.format("%,.2f", total), fTotalVal));
        tVal.setBackgroundColor(TEAL);
        tVal.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tVal.setPaddingRight(6f);
        totalInner.addCell(tVal);

        PdfPCell totalOuter = new PdfPCell(totalInner);
        totalOuter.setColspan(2);
        totalOuter.setBackgroundColor(TEAL);
        totalOuter.setBorder(Rectangle.NO_BORDER);
        totalOuter.setFixedHeight(30f);
        tbl.addCell(totalOuter);

        doc.add(tbl);
    }

    private void sumRow(PdfPTable tbl, String label, String value) {
        span2Blank(tbl);

        PdfPCell lc = noBorder(new Phrase(label, fSumLbl));
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setPaddingBottom(5f);
        tbl.addCell(lc);

        PdfPCell vc = noBorder(new Phrase(value, fSumVal));
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPaddingBottom(5f);
        vc.setPaddingRight(6f);
        tbl.addCell(vc);
    }

    private void span2Blank(PdfPTable tbl) {
        PdfPCell blank = noBorder(new Phrase(""));
        blank.setColspan(2);
        tbl.addCell(blank);
    }

    private void addFooter(Document doc) throws DocumentException {
        addSepLine(doc, BLUE_LINE, 1f, 20f);

        PdfPTable tbl = new PdfPTable(1);
        tbl.setWidthPercentage(100);

        Paragraph fp = new Paragraph(
                "Thank you for choosing CareLink!  ·  www.carelink.lk  ·  info@carelink.lk  ·  +94 11 234 5678",
                fFooter);
        fp.setAlignment(Element.ALIGN_CENTER);

        PdfPCell fc = new PdfPCell(fp);
        fc.setBackgroundColor(ROW_ALT);
        fc.setBorder(Rectangle.NO_BORDER);
        fc.setFixedHeight(32f);
        fc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        fc.setPaddingTop(8f);
        fc.setPaddingBottom(8f);
        tbl.addCell(fc);

        doc.add(tbl);
    }

    private void addSepLine(Document doc, BaseColor color, float height, float spacingBefore)
            throws DocumentException {
        PdfPTable tbl = new PdfPTable(1);
        tbl.setWidthPercentage(100);
        tbl.setSpacingBefore(spacingBefore);
        PdfPCell c = noBorder(new Phrase(""));
        c.setFixedHeight(height);
        c.setBackgroundColor(color);
        tbl.addCell(c);
        doc.add(tbl);
    }

    private Paragraph para(String text, Font font, int align) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(align);
        return p;
    }

    private PdfPCell noBorder(Phrase content) {
        PdfPCell c = new PdfPCell(content);
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String nvl(String s, String fallback) {
        return (s != null && !s.isEmpty()) ? s : fallback;
    }

    private double getItemsTotal(Order order) {
        double itemsSum = 0.0;

        if (order.getOrderItems() != null) {
            itemsSum += order.getOrderItems().stream()
                    .filter(i -> i.getProduct() != null)
                    .mapToDouble(i -> i.getProduct().getPrice() * i.getQuantity())
                    .sum();
        }

        if (order.getPrescriptions() != null) {
            itemsSum += order.getPrescriptions().stream()
                    .mapToDouble(p -> p.getPrice() != null ? p.getPrice() : 0.0)
                    .sum();
        }

        return itemsSum;
    }
}