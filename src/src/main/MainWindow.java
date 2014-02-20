package src.main;

import java.awt.EventQueue;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.awt.GridLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class MainWindow implements ClipboardOwner {

	private JFrame frame;
	private JTextField txtTextInput;
	private JTextField txtTextOutput;
	private JTextField textFieldTbName;
	private JTextField textFieldElement;
	private JTextField textFieldUnterelement;
	Boolean Datei = null;

	BufferedReader in;
	ArrayList<String> sqlZeilen = new ArrayList<String>();

	Document doc = null;

	FileWriter writer;
	File file;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow window = new MainWindow();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MainWindow() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 264);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(0, 2, 0, 0));

		JLabel lblInputfile = new JLabel("Inputfile:");
		frame.getContentPane().add(lblInputfile);

		txtTextInput = new JTextField();
		txtTextInput.setText("Basiselemente.mm");
		frame.getContentPane().add(txtTextInput);
		txtTextInput.setColumns(10);

		JLabel lblTbName = new JLabel("Tabellenname:");
		frame.getContentPane().add(lblTbName);

		textFieldTbName = new JTextField();
		textFieldTbName.setText("BasiselementDomaene");
		frame.getContentPane().add(textFieldTbName);
		textFieldTbName.setColumns(10);

		JLabel lblElementname = new JLabel("Elementname:");
		frame.getContentPane().add(lblElementname);

		textFieldElement = new JTextField();
		textFieldElement.setText("Basiselementname");
		frame.getContentPane().add(textFieldElement);
		textFieldElement.setColumns(10);

		JLabel lblUnterelementname = new JLabel("Unterelementname:");
		frame.getContentPane().add(lblUnterelementname);

		textFieldUnterelement = new JTextField();
		textFieldUnterelement.setText("UntergeordnetesBasiselement");
		frame.getContentPane().add(textFieldUnterelement);
		textFieldUnterelement.setColumns(10);

		JLabel label = new JLabel("Outputfile:");
		frame.getContentPane().add(label);

		txtTextOutput = new JTextField();
		txtTextOutput.setText("sqltest");
		frame.getContentPane().add(txtTextOutput);
		txtTextOutput.setColumns(10);

		JButton btnStart = new JButton("In Zwischenablage kopieren");
		btnStart.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String ergebnis = start();
				if (ergebnis.equals("true")) {
					schreiben(false);
				} else {
					JOptionPane.showMessageDialog(frame, ergebnis);
				}
			}

		});

		JButton btnAusgabeInDatei = new JButton("Ausgabe in Datei");
		btnAusgabeInDatei.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String ergebnis = start();
				if (ergebnis.equals("true")) {
					schreiben(true);
				} else {
					JOptionPane.showMessageDialog(frame, ergebnis);
				}
			}
		});
		frame.getContentPane().add(btnAusgabeInDatei);
		frame.getContentPane().add(btnStart);
	}

	public String start() {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			System.out.println(txtTextInput.getText());
			doc = dBuilder.parse(txtTextInput.getText());
			doc.getDocumentElement().normalize();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e) {
			return "Datei konnte nicht gefunden werden";
		} catch (SAXException e) {
			return "XML-Struktur ist nicht korrekt";
		}

		// Create a list of orders and sub elements
		Node Firstnode = doc.getDocumentElement();
		MyNode father = rekursion(Firstnode);
		//printNodes(father);
		sqlZeilen.clear();
		addSQLZeile("", "");
		sqlParser(father);
		return "true";
	}

	private void printNodes(MyNode firstnode) {
		if (firstnode == null)
			return;
		for (MyNode node : firstnode.subList) {
			System.out.println("Father " + firstnode.getText() + " son "
					+ node.getText());
			printNodes(node);
		}
	}

	private MyNode rekursion(Node node) {
		switch (node.getNodeName()) {
		case "map":
			return rekursion(node.getChildNodes().item(3));
		case "node":
			String text = "ERRROR -------";
			Node item = node.getAttributes().getNamedItem("TEXT");
			if (item == null) {
				NodeList childs = node.getChildNodes();
				for (int i = 0; i < childs.getLength(); i++)
					if (childs.item(i).getNodeName().equals("richcontent"))
						text = childs.item(i).getTextContent()
								.replaceAll("\n", "").trim();
			} else {
				text = item.getNodeValue();
			}
			text = replaceSpecialChar(text);
			MyNode father = new MyNode(text);
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child != null) {
					MyNode son = rekursion(child);
					if (son != null)
						father.addToSubList(son);
				}
			}
			return father;
		default :
			if (node.getNextSibling() == null)
				return null;
			return rekursion(node.getNextSibling());
		}
	}

	public void sqlParser(MyNode father) {
		addSQLZeile(father.getText(), "null");
		for (MyNode node : father.getSubList()) {
			addSQLZeile(father.getText(), node.getText());
			sqlParser(node);
		}
	}
	
	public void addSQLZeile (String father, String son) {
		String sqlZeile = "INSERT INTO " + textFieldTbName.getText() + " ("
				+ textFieldElement.getText() + ", "
				+ textFieldUnterelement.getText() + ") " + "VALUES ('"
				+ father + "', '" + son + "');";
		if (!sqlZeilen.contains(sqlZeile)) {
			sqlZeilen.add(sqlZeile);
		}
	}

	public void schreiben(Boolean inDatei) {
		if (inDatei) {
			file = new File(txtTextOutput.getText() + ".txt");
			file.delete();
			try {
				file.createNewFile();
				writer = new FileWriter(file, true);
				for (String zeile : sqlZeilen) {

					writer.write(zeile);
					// Platformunabhängiger Zeilenumbruch wird in den Stream
					// geschrieben
					writer.write(System.getProperty("line.separator"));
					writer.flush();
				}
				writer.close();
			} catch (IOException e) {
				JOptionPane
						.showMessageDialog(frame,
								"Datei konnte nicht geschrieben werden. Eventuell noch geöffnet?");
			}
		} else {
			String alles = "";
			for (String zeile : sqlZeilen) {
				alles = alles + zeile + System.getProperty("line.separator");

			}
			StringSelection stringSelection = new StringSelection(alles);
			Clipboard clipboard = Toolkit.getDefaultToolkit()
					.getSystemClipboard();
			clipboard.setContents(stringSelection, this);
		}
		JOptionPane.showMessageDialog(frame, sqlZeilen.size() + " Einträge");
	}


	public String replaceSpecialChar(String text) {
		return text.replace("&#xe4;", "ä").replace("&#xc4;", "Ä")
				.replace("&#xd6;", "Ö").replace("&#xf6;", "ö")
				.replace("&#xfc;", "ü").replace("&#xDC;", "Ü")
				.replace("&#xdf;", "ß").replace("&#xe9;", "é").trim();
	}

	@Override
	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// do nothing
	}

}
