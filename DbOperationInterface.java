import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * created an interface to implement in DbOperations for Database operations and
 * xml creation
 * 
 * @author Bharat
 *
 */
public interface DbOperationInterface {
	public void connectToSQL();

	public void runSqlQuery() throws IOException;

	public void writeDataToFile(String xmlFile, ArrayList<HashMap> custInfoList, String startDate, String endDate,
			int counter, String orderTotal, ArrayList<String> productLineList, ArrayList<HashMap> productInfoList,
			ArrayList<HashMap> employeeInfoList);
}
