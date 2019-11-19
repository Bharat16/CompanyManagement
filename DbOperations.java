import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DbOperations implements DbOperationInterface {
	Connection connect = null;
	Statement statement = null;
	ResultSet resultSet = null;
	BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
	String startDate, endDate, xmlFile;
	// Data Structure(HashMap) to store Customer Details
	HashMap<String, String> customerInfo;
	ArrayList<HashMap> customerInfoList = new ArrayList<HashMap>();

	// Data Structure(HashMap) to store Product Details
	HashMap<String, String> productInfo;
	ArrayList<HashMap> productInfoList = new ArrayList<HashMap>();
	ArrayList<String> productLineList = new ArrayList<String>();

	// Data Structure(HashMap) to store Employee Details
	HashMap<String, String> employeeInfo;
	ArrayList<HashMap> employeeInfoList = new ArrayList<HashMap>();
	int counter = 0;
	String orderTotal = "0";
	public static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	public static final String CONNECTION_URL = "jdbc:mysql://db.cs.dal.ca:3306?serverTimezone=UTC";
	public static final String USERNAME = "bbhargava";
	public static final String PASSWORD = "B00838511";

	/**
	 * to connect to the database
	 */
	public void connectToSQL() {
		try {
			Class.forName(JDBC_DRIVER);
			connect = DriverManager.getConnection(CONNECTION_URL, USERNAME, PASSWORD);
			statement = connect.createStatement();
			runSqlQuery();
		} catch (ClassNotFoundException | SQLException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * to run the required SQL queries and store the output to the data Structure
	 * defined above
	 */
	public void runSqlQuery() throws IOException {
		try {
			statement.execute("use csci3901;");
			// To get start date from user
			System.out.println("Enter start date in YYYY-MM-DD format");
			startDate = input.readLine();
			// To get end date from user
			System.out.println("Enter end date in YYYY-MM-DD format");
			endDate = input.readLine();
			System.out.println("Enter output xml file name");
			xmlFile = input.readLine();
			// query to fetch customer details
			String sqlQuery = "SELECT customerName,addressLine1 FROM customers WHERE customerNumber IN (SELECT customerNumber FROM orders WHERE orderDate BETWEEN '"
					+ startDate + "' AND '" + endDate + "' AND status != 'cancelled');";
			resultSet = statement.executeQuery(sqlQuery);
			counter = 0;

			// to check if result set is empty
			if (!resultSet.next()) {
				writeDataToFile(xmlFile, customerInfoList, startDate, endDate, counter, orderTotal, productLineList,
						productInfoList, employeeInfoList);
			} else {
				do {
					// store the customer details data into the data structure
					customerInfo = new HashMap<String, String>();
					customerInfo.put("customerName", resultSet.getString("customerName"));
					customerInfo.put("addressLine1", resultSet.getString("addressLine1"));
					customerInfoList.add(customerInfo);
					counter++;
				} while (resultSet.next());

				// query to fetch no. orders for the date range provided by user
				sqlQuery = "SELECT SUM(quantityOrdered * priceEach) AS orderTotal FROM orderdetails WHERE orderNumber IN (SELECT orderNumber FROM orders WHERE orderDate BETWEEN '"
						+ startDate + "' AND '" + endDate + "' AND status != 'cancelled');";
				resultSet = statement.executeQuery(sqlQuery);
				resultSet.next();
				orderTotal = resultSet.getString("orderTotal");

				// query to fetch product details
				sqlQuery = "SELECT productName,productLine,productVendor,SUM(quantityOrdered) AS unitsOrdered,SUM(quantityOrdered*priceEach) AS totalSales FROM products NATURAL JOIN orders NATURAL JOIN orderdetails WHERE orderDate BETWEEN '"
						+ startDate + "' AND '" + endDate + "' AND orders.status != 'cancelled' GROUP BY productCode ORDER BY productLine;";
				resultSet = statement.executeQuery(sqlQuery);
				while (resultSet.next()) {
					if (productLineList.size() == 0) {
						productLineList.add(resultSet.getString("productLine"));
					} else {
						boolean existFlag = false;
						for (String i : productLineList) {
							if (i.equals(resultSet.getString("productLine"))) {
								existFlag = true;
							}
						}

						if (!existFlag) {
							productLineList.add(resultSet.getString("productLine"));
						}
					}

					productInfo = new HashMap<String, String>();
					productInfo.put("productName", resultSet.getString("productName"));
					productInfo.put("productLine", resultSet.getString("productLine"));
					productInfo.put("productVendor", resultSet.getString("productVendor"));
					productInfo.put("unitsOrdered", resultSet.getString("unitsOrdered"));
					productInfo.put("totalSales", resultSet.getString("totalSales"));
					productInfoList.add(productInfo);
				}

				// query to fetch employee details
				sqlQuery = "SELECT firstName,lastName,offices.city,SUM(priceEach) AS totalSales,COUNT(distinct customerNumber) AS activeCustomers FROM orderdetails NATURAL JOIN orders NATURAL JOIN customers INNER JOIN employees ON employees.employeeNumber=customers.salesRepEmployeeNumber INNER JOIN offices ON offices.officeCode=employees.officeCode WHERE orderDate BETWEEN '"
						+ startDate + "' AND '" + endDate + "' AND orders.status != 'cancelled' GROUP BY employeeNumber;";
				resultSet = statement.executeQuery(sqlQuery);
				while (resultSet.next()) {
					employeeInfo = new HashMap<String, String>();
					employeeInfo.put("firstName", resultSet.getString("firstName"));
					employeeInfo.put("lastName", resultSet.getString("lastName"));
					employeeInfo.put("city", resultSet.getString("city"));
					employeeInfo.put("totalSales", resultSet.getString("totalSales"));
					employeeInfo.put("activeCustomers", resultSet.getString("activeCustomers"));
					employeeInfoList.add(employeeInfo);
				}

				writeDataToFile(xmlFile, customerInfoList, startDate, endDate, counter, orderTotal, productLineList,
						productInfoList, employeeInfoList);
			}

			resultSet.close();
			statement.close();
			connect.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * to write the data to an XML file in the required format
	 * 
	 * @param xmlFile
	 * @param custInfoList
	 * @param startDate
	 * @param endDate
	 * @param counter
	 * @param orderTotal
	 * @param productLineList
	 * @param productInfoList
	 * @param employeeInfoList
	 */
	public void writeDataToFile(String xmlFile, ArrayList<HashMap> customerInfoList, String startDate, String endDate,
			int counter, String orderTotal, ArrayList<String> productLineList, ArrayList<HashMap> productInfoList,
			ArrayList<HashMap> employeeInfoList) {
		// absolute path for xml file
		String xmlFilePath;
		if (xmlFile.endsWith(".xml") || xmlFile.endsWith(".XML")) {
			xmlFilePath = xmlFile;
		} else {
			xmlFilePath = xmlFile + ".xml";
		}
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		Document document = documentBuilder.newDocument();

		try {
			Element root = document.createElement("year_end_summary");
			document.appendChild(root);

			Element year = document.createElement("year");
			root.appendChild(year);
			Element startDateTag = document.createElement("start_date");
			startDateTag.appendChild(document.createTextNode(startDate));
			year.appendChild(startDateTag);
			Element endDateTag = document.createElement("end_date");
			endDateTag.appendChild(document.createTextNode(endDate));
			year.appendChild(endDateTag);

			Element customer_list = document.createElement("customer_list");
			root.appendChild(customer_list);

			Element customer = document.createElement("customer");
			customer_list.appendChild(customer);
			if (customerInfoList.isEmpty() == false) {
				for (HashMap i : customerInfoList) {
					Element customer_name = document.createElement("customer_name");
					customer_name.appendChild(document.createTextNode(i.get("customerName").toString()));
					customer.appendChild(customer_name);

					Element address = document.createElement("address");
					address.appendChild(document.createTextNode(i.get("addressLine1").toString()));
					customer.appendChild(address);
				}
			}

			// number of orders
			Element num_orders = document.createElement("num_orders");
			num_orders.appendChild(document.createTextNode(Integer.toString(counter)));
			customer.appendChild(num_orders);

			Element order_value = document.createElement("order_value");
			order_value.appendChild(document.createTextNode(orderTotal));
			customer.appendChild(order_value);

			// product details
			Element product_list = document.createElement("product_list");
			root.appendChild(product_list);
			Element product_set = document.createElement("product_set");
			product_list.appendChild(product_set);

			if (productLineList.isEmpty() == false && productInfoList.isEmpty() == false) {
				for (String i : productLineList) {
					Element product_line_name = document.createElement("product_line_name");
					product_line_name.appendChild(document.createTextNode(i));
					product_set.appendChild(product_line_name);

					for (HashMap h : productInfoList) {
						if (i.equals(h.get("productLine"))) {
							Element product = document.createElement("product");
							product_set.appendChild(product);

							Element product_name = document.createElement("product_name");
							product_name.appendChild(document.createTextNode(h.get("productName").toString()));
							product.appendChild(product_name);

							Element product_vendor = document.createElement("product_vendor");
							product_vendor.appendChild(document.createTextNode(h.get("productVendor").toString()));
							product.appendChild(product_vendor);

							Element units_sold = document.createElement("units_sold");
							units_sold.appendChild(document.createTextNode(h.get("unitsOrdered").toString()));
							product.appendChild(units_sold);

							Element total_sales = document.createElement("total_sales");
							total_sales.appendChild(document.createTextNode(h.get("totalSales").toString()));
							product.appendChild(total_sales);
						}
					}
				}
			}

			// List of employees
			Element staff_list = document.createElement("staff_list");
			root.appendChild(staff_list);

			if (employeeInfoList.isEmpty() == false) {
				for (HashMap h : employeeInfoList) {
					Element employee = document.createElement("employee");
					staff_list.appendChild(employee);

					Element first_name = document.createElement("first_name");
					first_name.appendChild(document.createTextNode(h.get("firstName").toString()));
					employee.appendChild(first_name);

					Element last_name = document.createElement("last_name");
					last_name.appendChild(document.createTextNode(h.get("lastName").toString()));
					employee.appendChild(last_name);

					Element office_city = document.createElement("office_city");
					office_city.appendChild(document.createTextNode(h.get("city").toString()));
					employee.appendChild(office_city);

					Element active_customers = document.createElement("active_customers");
					active_customers.appendChild(document.createTextNode(h.get("activeCustomers").toString()));
					employee.appendChild(active_customers);

					Element total_sales = document.createElement("total_sales");
					total_sales.appendChild(document.createTextNode(h.get("totalSales").toString()));
					employee.appendChild(total_sales);
				}
			}

			// to create the xml file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource domSource = new DOMSource(document);
			StreamResult streamResult = new StreamResult(new File(xmlFilePath));
			transformer.transform(domSource, streamResult);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

}
