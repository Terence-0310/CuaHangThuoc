package app;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import components.ProductTable;
import dao.ProductDao;
import common.ConnectDB;

/**
 * @author Generated
 * @version 1.0
 */
public class JframeProduct extends JFrame {

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;
    private JPanel panel;
    private JScrollPane scrollPane;
    private JTable table;
    private JComboBox<String> comboBox;
    private JButton btnFirst, btnLast, btnPrevious, btnNext;
    private JTextField txtpage;
    private JLabel lblpage;

    // paging
    private Integer pageNumber = 1;
    private Integer rowOfPage = 10;
    private Double totalPage = 0.0;
    private Integer totalRow = 0;

    /* ================= MAIN ================= */
    public static void main(String[] args) {
        // Đăng ký shutdown hook để đóng connection pool khi ứng dụng tắt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConnectDB.closeConnectionPool();
        }));

        try {
            UIManager.setLookAndFeel(
                "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        EventQueue.invokeLater(() -> {
            try {
                new JframeProduct().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Create the frame.
     */
    public JframeProduct() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        setTitle("Product");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(100, 100, 970, 576);
        
        // Đảm bảo đóng connection pool khi đóng frame
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                ConnectDB.closeConnectionPool();
            }
        });

        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(null);
        setContentPane(contentPane);

        panel = new JPanel();
        panel.setBorder(new TitledBorder(
                new LineBorder(Color.BLUE),
                "Product:",
                TitledBorder.LEADING,
                TitledBorder.TOP,
                null,
                Color.BLUE
        ));
        panel.setBounds(36, 68, 607, 427);
        panel.setLayout(null);
        contentPane.add(panel);

        scrollPane = new JScrollPane();
        scrollPane.setBounds(27, 36, 544, 287);
        panel.add(scrollPane);

        table = new JTable();
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(60);
        scrollPane.setViewportView(table);

        comboBox = new JComboBox<>();
        comboBox.setModel(new DefaultComboBoxModel<>(new String[]{"10", "20", "50", "100"}));
        comboBox.setBounds(246, 334, 105, 22);
        comboBox.addActionListener(this::comboBoxActionPerformed);
        panel.add(comboBox);

        btnFirst = new JButton("First");
        btnFirst.setBounds(27, 334, 97, 23);
        btnFirst.addActionListener(this::btnFirstActionPerformed);
        panel.add(btnFirst);

        btnPrevious = new JButton("Previous");
        btnPrevious.setBounds(137, 334, 97, 23);
        btnPrevious.addActionListener(this::btnPreviousActionPerformed);
        panel.add(btnPrevious);

        btnNext = new JButton("Next");
        btnNext.setBounds(363, 334, 97, 23);
        btnNext.addActionListener(this::btnNextActionPerformed);
        panel.add(btnNext);

        btnLast = new JButton("Last");
        btnLast.setBounds(472, 334, 97, 23);
        btnLast.addActionListener(this::btnLastActionPerformed);
        panel.add(btnLast);

        txtpage = new JTextField("1");
        txtpage.setHorizontalAlignment(SwingConstants.CENTER);
        txtpage.setBounds(246, 367, 106, 20);
        txtpage.addActionListener(this::txtpageActionPerformed);
        panel.add(txtpage);

        lblpage = new JLabel("page 0/0");
        lblpage.setBounds(27, 368, 120, 14);
        panel.add(lblpage);

        if (!java.beans.Beans.isDesignTime()) {
            loadProduct();
        }
    }

    // viết phương thức để đỗ dữ liệu vào bảng
    private void loadProduct() {
        ProductDao dao = new ProductDao();

        totalRow = dao.countProduct();
        totalPage = Math.ceil(totalRow.doubleValue() / rowOfPage.doubleValue());
        lblpage.setText("page " + pageNumber + "/" + totalPage.intValue());

        ProductTable model = new ProductTable();

        dao.showProducts(pageNumber, rowOfPage).forEach(pro -> {
            ImageIcon img = null;
            try {
                img = new ImageIcon(pro.getProimg().trim());
                Image icon = img.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                img = new ImageIcon(icon);
            } catch (Exception e) {
                img = null;
            }

            model.addRow(new Object[]{
                pro.getProid(),
                pro.getProname(),
                pro.isProstatus(),
                pro.getPromfg(),
                img
            });
        });

        table.setModel(model);
    }

    private void refreshProduct() {
        ProductTable model = (ProductTable) table.getModel();
        model.setRowCount(0);

        ProductDao dao = new ProductDao();
        totalRow = dao.countProduct();
        totalPage = Math.ceil(totalRow.doubleValue() / rowOfPage.doubleValue());
        lblpage.setText("page " + pageNumber + "/" + totalPage.intValue());

        dao.showProducts(pageNumber, rowOfPage).forEach(pro -> {
            ImageIcon img = null;
            try {
                img = new ImageIcon(pro.getProimg().trim());
                Image icon = img.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                img = new ImageIcon(icon);
            } catch (Exception e) {
                img = null;
            }

            model.addRow(new Object[]{
                pro.getProid(),
                pro.getProname(),
                pro.isProstatus(),
                pro.getPromfg(),
                img
            });
        });
    }

    protected void btnNextActionPerformed(ActionEvent e) {
        if (pageNumber < totalPage.intValue()) {
            pageNumber++;
            txtpage.setText(pageNumber.toString());
            refreshProduct();
        }
    }

    protected void btnPreviousActionPerformed(ActionEvent e) {
        if (pageNumber > 1) {
            pageNumber--;
            txtpage.setText(pageNumber.toString());
            refreshProduct();
        }
    }

    protected void btnLastActionPerformed(ActionEvent e) {
        pageNumber = totalPage.intValue();
        txtpage.setText(pageNumber.toString());
        refreshProduct();
    }

    protected void btnFirstActionPerformed(ActionEvent e) {
        pageNumber = 1;
        txtpage.setText(pageNumber.toString());
        refreshProduct();
    }

    protected void comboBoxActionPerformed(ActionEvent e) {
        if (table != null) {
            pageNumber = 1;
            rowOfPage = Integer.parseInt(comboBox.getSelectedItem().toString());
            txtpage.setText(pageNumber.toString());
            refreshProduct();
        }
    }

    protected void txtpageActionPerformed(ActionEvent e) {
        String str = txtpage.getText().trim();

        if (!str.matches("\\d+")) {
            txtpage.setText(pageNumber.toString());
            JOptionPane.showMessageDialog(this, "Invalid page number!");
            return;
        }

        int p = Integer.parseInt(str);
        if (p >= 1 && p <= totalPage.intValue()) {
            pageNumber = p;
            txtpage.setText(pageNumber.toString());
            refreshProduct();
        } else {
            txtpage.setText(pageNumber.toString());
            JOptionPane.showMessageDialog(this, "Invalid page number!");
        }
    }
}
