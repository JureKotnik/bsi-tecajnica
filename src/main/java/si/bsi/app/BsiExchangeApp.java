package si.bsi.app;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.w3c.dom.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Namizna aplikacija za analizo tečajnice.
 * * DELOVANJE:
 * 1. Branje podatkov: Ob zagonu se izvede neposreden prenos XML datoteke z BSI strežnika.
 * Uporabljen je standardni Java DOM parser. Podatki se shranijo v hierarhično strukturo
 * Map<Datum, Map<Valuta, Tečaj>> za hiter dostop in filtriranje.
 * 2. Uporabniški vmesnik: Zgrajen s knjižnico Swing. Sestavljen je iz zgornje nadzorne plošče
 * za vnos parametrov ter osrednjega razdeljenega pogleda za tabelo in JFreeChart graf.
 * 3. Analiza: Metoda performAnalysis() prefiltrira podatke glede na izbran časovni okvir,
 * osveži prikaz in prikaže vrste na grafu.
 * 4. Izračun: Na podlagi prvega in zadnjega prebranega tečaja v izbranem obdobju
 * se izračuna prihodek za začetni vložek 1000 EUR in prikaže, katera valuta bi bila boljša izbira.
 */
public class BsiExchangeApp extends JFrame {

    private final Map<String, Map<String, Double>> exchangeRates = new TreeMap<>();
    private final Set<String> availableCurrencies = new TreeSet<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private JComboBox<String> currency1Box, currency2Box;
    private JTextField startDateField, endDateField;
    private DefaultTableModel tableModel;
    private JPanel chartContainer;
    private JLabel opportunityResultLabel;

    public BsiExchangeApp() {
        setTitle("BSI Tečajnica in Analiza");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        loadBsiData();
        initUI();
    }

    private void loadBsiData() {
        try {
            System.out.println("Prenasam podatke BSI...");
            URL url = new URL("https://www.bsi.si/_data/tecajnice/dtecbs-l.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(url.openStream());

            NodeList daysList = doc.getElementsByTagName("tecajnica");
            for (int i = 0; i < daysList.getLength(); i++) {
                Element dayElement = (Element) daysList.item(i);
                String date = dayElement.getAttribute("datum");
                
                Map<String, Double> rates = new HashMap<>();
                NodeList rateList = dayElement.getElementsByTagName("tecaj");
                
                for (int j = 0; j < rateList.getLength(); j++) {
                    Element rateElement = (Element) rateList.item(j);
                    String currency = rateElement.getAttribute("oznaka");
                    double rate = Double.parseDouble(rateElement.getTextContent());
                    rates.put(currency, rate);
                    availableCurrencies.add(currency);
                }
                exchangeRates.put(date, rates);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Napaka pri prenosu podatkov: " + e.getMessage(), "Napaka", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initUI() {
        JPanel controlPanel = new JPanel(new FlowLayout());
        
        controlPanel.add(new JLabel("Od (Leto-Mesec-Dan):"));
        startDateField = new JTextField("2023-01-01", 8);
        controlPanel.add(startDateField);
        
        controlPanel.add(new JLabel("Do:"));
        endDateField = new JTextField("2024-01-01", 8);
        controlPanel.add(endDateField);

        currency1Box = new JComboBox<>(availableCurrencies.toArray(new String[0]));
        currency2Box = new JComboBox<>(availableCurrencies.toArray(new String[0]));
        if(availableCurrencies.contains("USD")) currency1Box.setSelectedItem("USD");
        if(availableCurrencies.contains("CHF")) currency2Box.setSelectedItem("CHF");

        controlPanel.add(new JLabel("Valuta 1:"));
        controlPanel.add(currency1Box);
        controlPanel.add(new JLabel("Valuta 2:"));
        controlPanel.add(currency2Box);

        JButton analyzeBtn = new JButton("Analiziraj");
        analyzeBtn.addActionListener(e -> performAnalysis());
        controlPanel.add(analyzeBtn);

        add(controlPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        tableModel = new DefaultTableModel(new String[]{"Datum", "Valuta 1", "Valuta 2"}, 0);
        JTable table = new JTable(tableModel);
        splitPane.setTopComponent(new JScrollPane(table));

        chartContainer = new JPanel(new BorderLayout());
        splitPane.setBottomComponent(chartContainer);
        splitPane.setDividerLocation(250);

        add(splitPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel();
        opportunityResultLabel = new JLabel("Prikaz rezultatov.");
        bottomPanel.add(opportunityResultLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void performAnalysis() {
        String start = startDateField.getText().trim();
        String end = endDateField.getText().trim();
        String c1 = (String) currency1Box.getSelectedItem();
        String c2 = (String) currency2Box.getSelectedItem();

        tableModel.setRowCount(0);
        chartContainer.removeAll();

        TimeSeries series1 = new TimeSeries(c1);
        TimeSeries series2 = new TimeSeries(c2);

        double startRate1 = -1, startRate2 = -1;
        double endRate1 = -1, endRate2 = -1;

        for (Map.Entry<String, Map<String, Double>> entry : exchangeRates.entrySet()) {
            String dateStr = entry.getKey();
            if (dateStr.compareTo(start) >= 0 && dateStr.compareTo(end) <= 0) {
                Double r1 = entry.getValue().get(c1);
                Double r2 = entry.getValue().get(c2);

                if (r1 != null && r2 != null) {
                    tableModel.addRow(new Object[]{dateStr, r1, r2});
                    
                    try {
                        Date d = dateFormat.parse(dateStr);
                        series1.addOrUpdate(new Day(d), r1);
                        series2.addOrUpdate(new Day(d), r2);
                        
                        if (startRate1 == -1) {
                            startRate1 = r1; startRate2 = r2;
                        }
                        endRate1 = r1; endRate2 = r2;

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(series1);
        dataset.addSeries(series2);

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Gibanje tečajev za izbrano obdobje", "Datum", "Tečaj (za 1 EUR)", dataset, true, true, false);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartContainer.add(chartPanel, BorderLayout.CENTER);
        chartContainer.validate();

        if (startRate1 != -1 && endRate1 != -1) {
            double eurInv = 1000.0;
            double amountC1 = eurInv * startRate1;
            double amountC2 = eurInv * startRate2;

            double backToEur1 = amountC1 / endRate1;
            double backToEur2 = amountC2 / endRate2;

            double diff = backToEur1 - backToEur2;
            String better = diff > 0 ? c1 : c2;
            
            opportunityResultLabel.setText(String.format(
                "Rezultat za 1000€: Izbira %s bi prinesla %.2f EUR več kot %s.", 
                better, Math.abs(diff), (better.equals(c1) ? c2 : c1)
            ));
        } else {
            opportunityResultLabel.setText("Ni dovolj podatkov za izračun izgube/dobička.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BsiExchangeApp().setVisible(true);
        });
    }
}