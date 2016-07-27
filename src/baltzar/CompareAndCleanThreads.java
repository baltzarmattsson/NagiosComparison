package baltzar;

import jxl.write.*;
import jxl.write.Number;
import org.jsoup.Jsoup;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import org.jsoup.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import jxl.Workbook;

import baltzar.SplitAndSearch;

/**
 * Created by Baltzar on 2016-07-13.
 *
 * Edit: C-P from the file CompareNewOldServicesWithTvätt. See local file on Linux machine
 */


public class CompareAndCleanThreads {

    private static int rowCountOriginalFile;

    private static HashMap<String, Holder> indexesOriginal;
    private static HashMap<String, Holder> indexesNew;
    private static String dir;
    private static String outputFileURLText;
    private static String originalHTMLFile;
    private static String newHTMLFile;
    private static String oldFileText;
    private static String newFileText;
    private static String originalFileCorrectIndexes;
    private static String newFileCorrectIndexes;
    private static String excelFileURL;
    private static int numberOfColumns;

    //
    private static String emailpass;
    //

    public static void main(String[] args) {

        dir = "/tmp/nagiosdiff/";
        //dir = "/Users/baltzarmattsson/txt/";

        outputFileURLText = dir + "Comparison.txt";

        Scanner scanner = new Scanner(System.in);
        System.out.print("[?] Send finished report via email? Y/N ");

        String answer = scanner.nextLine();
        String email = "";
        boolean send = false;
        if (answer.equalsIgnoreCase("y")) {
            send = true;
            System.out.println("[+] To email: ");
            email = scanner.nextLine();
            if (!email.contains("@cybercom.com")) {
                System.out.println("[!] Email is external, only @cybercom.com permitted");
                System.exit(0);
            }
        } else if (answer.equalsIgnoreCase("n")) {
            System.out.println("[-] No email will be sent");
            send = false;
        }
        scanner.close();
        System.out.println("[+] Starting analysis...");

        try {

            GetAndCleanNagiosFiles();
            GetColumnInfo();
            CreateFilesWithCorrectIndexes();
            Compare(); // org new output
            //BuildExcelFile();
            //if (send) mail(email);
            //else System.out.println("[+] COMPARISON COMPLETE [+]\n---------------------------");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }


    public static class Holder {

        private int index;
        private Keep keep;

        public Holder(int index) {
            this.index = index;
            this.keep = Keep.NO;
        }

        public int getIndex() {
            return index;
        }
        public Keep getKeep() {
            return this.keep;
        }
        public void setKeep(Keep keep) {
            this.keep = keep;
        }

    }

    public enum Keep {
        YES, NO, BOTH_COLS_EXIST, ALL_VALUES_IN_ORIGINAL_SAME_AND_BOTH_COLS_EXIST
    }



    public static void GetAndCleanNagiosFiles() {

        // Return: [0] = original, [1] = new

        originalHTMLFile = dir + "gamlaservices.html";
        oldFileText = dir + "gamlaservicescleaned.txt";
        newHTMLFile = dir + "nyaservices.html";
        newFileText = dir + "nyaservicescleaned.txt";

        BufferedWriter bw = null;
        StringBuilder builder = null;
        try {

            // Getting the files from the site



            /*** Getting PWs****/

            BufferedReader passread = new BufferedReader(new FileReader((new File("/home/studentpoolen/IdeaProjects/NagiosComparison/src/baltzar/creds"))));
            String frontcred = passread.readLine();
            String omdcred = passread.readLine();
            emailpass = passread.readLine();

            /*** //PWS ***/

            Runtime.getRuntime().exec("mkdir -p " + dir); // creates the directory even though the subfolders doesnt exist before
            Runtime.getRuntime().exec("curl --user " + frontcred +" http://192.168.90.36/cgi-bin/nagios3/config.cgi?type=services -o " + originalHTMLFile); // final frontier
            Runtime.getRuntime().exec("curl --user " + omdcred + " http://192.168.91.10/prod/thruk/cgi-bin/config.cgi?type=services -o " + newHTMLFile); // omgd


            // Read the new nagios file and remove the stuff we don't want
            // TODO ta bort kommentarer
            Thread.sleep(5000); // Sleep for 5 seconds to wait for the files to load/appear in the folder

            for (int times = 0; times < 2; times++) {

                Document doc = null;
                Elements table = null;

                // NEW Nagios reading
                if (times == 0) {
                    doc = Jsoup.parse(new File(newHTMLFile), "UTF-8");
                    bw = new BufferedWriter(new FileWriter(new File(newFileText)));
                    table = doc.select("#configtable"); // Getting the table in file

                    // OLD Nagios reading
                } else if (times == 1) {
                    doc = Jsoup.parse(new File(originalHTMLFile), "UTF-8");
                    bw = new BufferedWriter(new FileWriter(new File(oldFileText)));
                    table = doc.select("table[BORDER=0][CLASS='data']"); // Getting the table in file
                }

                builder = new StringBuilder();



                // Getting the column names
                Elements columnNames = table.select("th");
                for (int i = 1; i < columnNames.size(); i++) { // i = 1 because we don't want index[0] = "Service"
                    String colNameAtI = columnNames.get(i).text().replaceAll("\\<.([^>]*)\\>", ""); //Replaces everything that begins with < and ends with >, inclusive, with ""
                    if (colNameAtI.contains("\n")) colNameAtI = colNameAtI.replaceAll("\n", "");
                    builder.append(colNameAtI + "\t");
                }
                bw.write(builder.toString());
                builder.setLength(0);


                // Getting the table rows
                Elements rows = table.select("tr");
                for (int i = 0; i < rows.size(); i++) {
                    Element row = rows.get(i);
                    Elements cols = row.select("td");
                    for (int j = 0; j < cols.size(); j++) {
                        String rowDataAtJ = cols.get(j).text().replaceAll("\\<.([^>]*)\\>", "");

                        builder.append(rowDataAtJ + "\t");
                    }
                    bw.write(builder.toString() + "\n");
                    builder.setLength(0);
                }
                if (bw != null) bw.close();
            }

        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bw != null)
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        System.out.println("[+] Both files cleaned without errors...");
    }

    public static void GetColumnInfo() {

        System.out.println("[+] Removing unecessary columns...");
        File originalFile = new File(oldFileText);
        File newFile = new File(newFileText);
        BufferedReader br = null;
        BufferedWriter bw = null;
        StringBuilder builder = new StringBuilder();

        try {
            /**** 	--ORIGINAL FILE--	****/
            // Reading of the original file

            indexesOriginal = new HashMap<String, Holder>();
            br = new BufferedReader(new FileReader(originalFile));

            String orgLine = "";
            String temp = "";
            while ((temp = br.readLine()) != null) {
                if (temp.contains("Host") && temp.contains("Description")) {
                    orgLine = temp.replace("Default Contacts/Groups", "Contacts"); // since the new and old differs in col-name
                    break;
                }
            }
            if (orgLine.length() == 0) {
                System.out.println("Error: Could not find column headers for original file");
                System.exit(0);
            }

            // Getting the indexes of each column
            String[] sa = orgLine.split("\t");
            int numberOfColumnsOriginal = 0;
            for (int i = 0; i < sa.length; i++) {
                indexesOriginal.put(sa[i], new Holder(i));
                numberOfColumnsOriginal++;
            }
            /**** 	--END OF - ORIGINAL FILE--	****/


            /**** 	--NEW FILE--	****/
            // Reading of the new file
            indexesNew = new HashMap<String, Holder>();
            br = new BufferedReader(new FileReader(newFile));

            String newLine = "";
            temp = "";
            while ((temp = br.readLine()) != null) {
                if (temp.contains("Host") && temp.contains("Description")) {
                    newLine = temp;
                    break;
                }
            }

            if (newLine.length() == 0) {
                System.out.println("Error: Could not find column headers for new file");
                System.exit(0);
            }

            // Getting the indexes of each column
            sa = newLine.split("\t");
            int numberOfColumnsNew = 0;
            for (int i = 0; i < sa.length; i++) {
                indexesNew.put(sa[i], new Holder(i));
                numberOfColumnsNew++;
            }

            /**** 	--END OF - NEW FILE--	****/

            /*** Comparing which columns are in the old one but not in the new.
             *  All Keeps are created with a default value of Keep.NO, this way they have to "prove" themselves to be kept
             //      If a column in the NEW is not in the OLD, its Holder.keep is marked as FALSE (since we don't need to compare new values that didn't exists before)
             //      If a column exists in both, keep is marked as BOTH_COLS_EXIST
             //      If a column contains the same value in all rows, in both old and new, the keep is marked as FALSE, since we have no use for that data
             ***/

            // Remove the columns in the new file that doesnt exists in old:
            for (Map.Entry<String, Holder> orgMap : indexesOriginal.entrySet()) {
                String orgName = orgMap.getKey();
                Holder orgHold = orgMap.getValue();
                for (Map.Entry<String, Holder> newMap : indexesNew.entrySet()) {
                    String newName = newMap.getKey();
                    Holder newHold = newMap.getValue();

                    //If column exists in both, set keep to Keep.BOTH_COLS_EXIST
                    if (newName.contains(orgName) || orgName.contains(newName)) {
                        orgHold.setKeep(Keep.BOTH_COLS_EXIST);
                        indexesOriginal.put(orgName, orgHold);
                        newHold.setKeep(Keep.BOTH_COLS_EXIST);
                        indexesNew.put(newName, newHold);
                        break;
                    }
                }
            }

            // Check all rows in ORIGINAL FILE, if all rows of a column have the same value, we mark keep = false

            HashMap<String, ArrayList<String>> orgVals = new HashMap<String, ArrayList<String>>();
            // Fill the arraylist on each ColumnName with a new ArrayList to later hold the values of the rows in original file
            for (String s : indexesOriginal.keySet()) orgVals.put(s, new ArrayList<String>());

            HashMap<String, ArrayList<String>> newVals = new HashMap<String, ArrayList<String>>();
            // The same as above, but for the new file
            for (String s : indexesNew.keySet()) newVals.put(s, new ArrayList<String>());

            // READING OF ORIGINAL FILE COLUMN VALUES
            br = new BufferedReader(new FileReader(originalFile));
            String[] currentLine;
            String s;
            br.readLine(); // to get rid of column names
            while ((s = br.readLine()) != null) {
                while (s.equals("")) s = br.readLine();

                rowCountOriginalFile++;
                currentLine = new String[numberOfColumnsOriginal];
                String[] tmp = s.split("\t");
                for (int i = 0; i < numberOfColumnsOriginal-1; i++) {
                    currentLine[i] = (i < numberOfColumnsOriginal && i > tmp.length-1) ? "" : tmp[i];
                }
                // For each line we read from the file, we add the values to the respective ArrayList for that column, thus we get all values from all columns in separate ArrayLists
                for (Map.Entry<String, Holder> entry : indexesOriginal.entrySet()) {
                    String key = entry.getKey();
                    Keep keep = entry.getValue().getKeep();
                    int colIndex = entry.getValue().getIndex();
                    ArrayList<String> a = orgVals.get(key);
                    if (keep != Keep.NO) a.add(currentLine[colIndex]);
                }
            }

            // READING OF NEW FILE COLUMN VALUES
            br = new BufferedReader(new FileReader(newFile));
            br.readLine(); // to get rid of column names
            while ((s = br.readLine()) != null) {
                while (s.equals("")) s = br.readLine(); // if the line is blank
                currentLine = new String[numberOfColumnsNew];
                String[] tmp = s.split("\t");
                for (int i = 0; i < numberOfColumnsNew; i++) {
                    currentLine[i] = (i < numberOfColumnsNew && i > tmp.length - 1) ? "" : tmp[i];
                }
                // Same as above, fill the values of each column
                for (Map.Entry<String, Holder> entry : indexesNew.entrySet()) {
                    String key = entry.getKey();
                    Keep keep = entry.getValue().getKeep();
                    int colIndex = entry.getValue().getIndex();
                    ArrayList<String> a = newVals.get(key);
                    if (keep != Keep.NO) a.add(currentLine[colIndex]);
                }
            }

            // Compare both files and their values, if all values (of columns that exists in both old and new file) are the SAME, we set Keep.NO, else Keep.YES
            for (Map.Entry<String, Holder> map : indexesOriginal.entrySet()) {
                String key = map.getKey();
                Keep keep = map.getValue().getKeep();
                if (keep != Keep.NO) {
                    ArrayList<String> orgColVals = orgVals.get(key);
                    String sOrg = orgColVals.get(0).replaceAll("\u00a0", " ").trim();
                    Holder tempHoldOrg = indexesOriginal.get(key);
                    for (String sOrgLoop : orgColVals) {
                        if (!sOrg.equals(sOrgLoop)) {
                            tempHoldOrg.setKeep(Keep.ALL_VALUES_IN_ORIGINAL_SAME_AND_BOTH_COLS_EXIST);
                        }
                    }

                    Holder tempHoldNew = null;
                    if (newVals.get(key) != null && newVals.get(key).size() != 0) {
                        ArrayList<String> newColVals = newVals.get(key);
                        String sNew = newColVals.get(0).replaceAll("\u00a0"," ").trim();
                        tempHoldNew = indexesNew.get(key);
                        for (String sNewLoop : newColVals) {

                            if (!sNew.equals(sNewLoop) || !sNew.equals(sOrg)) {

                                tempHoldNew.setKeep(Keep.YES); // By now, we know that both columns exist, and the values are not the same in the old, new, and compared to eachother, therefore we keep it
                                tempHoldOrg.setKeep(Keep.YES);
                            }
                        }
                    }
                    if (tempHoldOrg.getKeep() == Keep.BOTH_COLS_EXIST || tempHoldOrg.getKeep() == Keep.ALL_VALUES_IN_ORIGINAL_SAME_AND_BOTH_COLS_EXIST) tempHoldOrg.setKeep(Keep.NO);
                    if (tempHoldNew != null && tempHoldNew.getKeep() == Keep.BOTH_COLS_EXIST) tempHoldNew.setKeep(Keep.NO);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
                if (bw != null) bw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        if (indexesOriginal == null || indexesNew == null) {
            System.out.println("index-hashmap(s) are null");
            System.exit(0);
        }

        numberOfColumns = 0;
        for (Map.Entry<String, Holder> holder : indexesOriginal.entrySet()) {
            if (holder.getValue().getKeep() == Keep.YES) {
                numberOfColumns++;
            }
        }


        System.out.println("[+] Unnecessary columns removed from analysis...");
    }

    public static void CreateFilesWithCorrectIndexes() {
        BufferedReader br = null;
        BufferedWriter bw = null;
        StringBuilder builder = new StringBuilder();
        String s = "";
        try {

            int max = 0;
            HashMap<Integer, String> tempColumnOrder = new HashMap<Integer, String>();
            for (Map.Entry<String, Holder> holder : indexesOriginal.entrySet()) {
                if (holder.getValue().getKeep() == Keep.YES) {
                    tempColumnOrder.put(holder.getValue().getIndex(), holder.getKey());
                }
                if (holder.getValue().getIndex() > max) {
                    max = holder.getValue().getIndex();
                }
            }

            String[] columnOrder = new String[numberOfColumns];
            int correctIndex = 0;
            for (int i = 0; i < max; i++) {
                if (tempColumnOrder.get(i) != null) {
                    columnOrder[correctIndex] = tempColumnOrder.get(i);
                    builder.append(tempColumnOrder.get(i) + "\t");
                    correctIndex++;
                }
            }

            for (int times = 0; times < 2; times++) {

                if (times == 0) {
                    br = new BufferedReader(new FileReader(new File(newFileText)));
                    newFileCorrectIndexes = "newFileCorrectIndexes.txt";
                    bw = new BufferedWriter(new FileWriter(new File(dir + newFileCorrectIndexes)));

                } else {
                    br = new BufferedReader(new FileReader(new File(oldFileText)));
                    originalFileCorrectIndexes = "originalFileCorrectIndexes.txt";
                    bw = new BufferedWriter(new FileWriter(new File(dir + originalFileCorrectIndexes)));
                }

                String[] sSplit;
                while ((s = br.readLine()) != null) {
                    while (s.equals("")) s = br.readLine(); // rid of blank rows

                    sSplit = s.split("\t");
                    for (int j = 0; j < sSplit.length; j++)
                        sSplit[j] = sSplit[j].replaceAll("\u00a0", "").trim();

                    for (int i = 0; i < numberOfColumns; i++) {
                        String key = columnOrder[i];
                        int index = (times == 0) ? indexesNew.get(key).getIndex() :
                                indexesOriginal.get(key).getIndex();

                        String colValue = "";
                        try {
                            colValue = (index > sSplit.length-1 || sSplit[index] == null) ? ""
                                    : sSplit[index].replaceAll("\u00a0"," ").trim();
                        } catch (ArrayIndexOutOfBoundsException ae) {
                            colValue = "";
                        }

                        if (key.equals("Contacts") && times == 1) {
                            colValue = colValue.replace("hosting-beredskap", "beredskap");
                            colValue = colValue.replace("hosting-cs", "service-desk");
                        }
                        builder.append(colValue + "\t");
                    }
                    bw.write(builder.toString() + "\n");
                    builder.setLength(0);
                }
                bw.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) bw.close();
                if (br != null) br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void Compare() throws IOException {

        System.out.println(dir + originalFileCorrectIndexes);
        File originalFile = new File(dir + originalFileCorrectIndexes);
        File newFile = new File(newFileCorrectIndexes);
        File outputFile = new File(outputFileURLText);
        BufferedReader br = null;
        BufferedReader br2 = null;
        BufferedWriter bw = null;
        StringBuilder builder = new StringBuilder();
        StringBuilder builder2 = new StringBuilder();

        br = new BufferedReader(new FileReader(originalFile));
        bw = new BufferedWriter(new FileWriter(outputFile));

            int max = 0;
            HashMap<Integer, String> tempColumnOrder = new HashMap<Integer, String>();
            for (Map.Entry<String, Holder> holder : indexesOriginal.entrySet()) {
                if (holder.getValue().getKeep() == Keep.YES)
                    tempColumnOrder.put(holder.getValue().getIndex(), holder.getKey());
                if (holder.getValue().getIndex() > max) max = holder.getValue().getIndex();
            }

            String[] columnOrder = new String[numberOfColumns];
            int correctIndex = 0;
            for (int i = 0; i < max; i++) {
                if (tempColumnOrder.get(i) != null) {
                    columnOrder[correctIndex] = tempColumnOrder.get(i);
                    builder.append(tempColumnOrder.get(i) + "\t");
                    correctIndex++;
                }
            }
            builder.append("Beredskap\t"
                    + "Beredskap i Service\t"
                    + "Exists Fully\t"
                    + "Exists Partially\t"
                    + "Diff On Col(s)\n");
            bw.write(builder.toString());
            builder.setLength(0);

            String originalLine = "";
            br.readLine(); // to get rid of the column names

            int rowsRead = 1;
            System.out.println("[+] Comparing files");

            boolean beredskap = false;
            boolean beredskapService = false;
            boolean existsFully = false;
            boolean existsPartially = false;
            String diffLocationColumnIndex = "";


            while ((originalLine = br.readLine()) != null) {

                /*** COOL PRINTBOY_96***/
                DecimalFormat df = new DecimalFormat("0.00");
                double d = (rowsRead * 100.0) / (double) rowCountOriginalFile;

                for (int i = 0; i < d; i++) builder.append("=");
                builder.append("> " + df.format(d) + "% done");
                String anim = "|/-\\";
                System.out.write(("\r[" + anim.charAt((int) (d % anim.length())) + "]" + builder.toString()).getBytes());
                builder.setLength(0);

                if (d == 100.0) System.out.write(("\n\r[+] 100.0% done!").getBytes());
                /*** PRINT ***/


                Object[] returnvalues = SplitAndSearch.splitAndSearch(dir + newFileCorrectIndexes, dir, originalLine, numberOfColumns);

                String searchResult = (String) returnvalues[0];
                existsFully = (boolean) returnvalues[1];
                existsPartially = (boolean) returnvalues[2];

                if (!existsFully && existsPartially) {
                    String[] orgArray = originalLine.split("\t");
                    String[] newArray = searchResult.split("\t");
                    for (int i = 0; i < numberOfColumns; i++) {
                        String columnName = columnOrder[i];
                        String colOrg = (i > orgArray.length - 1) ? "" : orgArray[i].replaceAll("\u00a0", " ").trim();
                        String colNew = (i > newArray.length - 1) ? "" : newArray[i].replaceAll("\u00a0", " ").trim();
                        if (!colOrg.equals(colNew)) {
                            builder.append(diffLocationColumnIndex);
                            builder.append((columnName + "=(old: \"" + colOrg + "\", new: \"" + colNew + "\")" + ", "));
                            diffLocationColumnIndex = builder.toString();
                            builder.setLength(0);
                        }
                    }
                }


                builder.setLength(0);
                builder.append(originalLine);
                builder.append((beredskap) ? "\ttrueBL\t" : "\tfalseBL\t");
                builder.append((beredskapService) ? "trueBL-S\t" : "falseBL-S\t");
                builder.append((existsFully) ? "trueFully\t" : "falseFully\t");
                builder.append((existsPartially) ? "truePart" : "falsePart");
                if (diffLocationColumnIndex.length() >= 2)
                    builder.append("\t" + diffLocationColumnIndex.substring(0, diffLocationColumnIndex.length() - 2)); // -2 to get rid of ", "
                builder.append("\n");
                bw.write(builder.toString());
                builder.setLength(0);
                diffLocationColumnIndex = "";


                /*** ***/
                rowsRead++;
                /*** ***/
            }


        if (br != null) br.close();
        if (br2 != null) br.close();
        if (bw != null) bw.close();

        System.out.println("\n[+] Comparison successfully completed!");
}

    public static String BuildExcelFile() {

        System.out.println("[+] Building excel file...");

        BufferedReader br = null;
        excelFileURL = dir + "ComparisonExcel.xls";
        try {
            Runtime.getRuntime().exec("mkdir " + dir);

            WritableWorkbook wworkbook;
            wworkbook = Workbook.createWorkbook(new File(excelFileURL));
            WritableSheet wsheet = wworkbook.createSheet("Sheet", 0);
            br = new BufferedReader(new FileReader(outputFileURLText));

            String s;
            int row = 0;
            while ((s = br.readLine()) != null) {
                while (s.length() == 0) s = br.readLine();

                String[] currentLine = s.split("\t");

                for (int col = 0; col < currentLine.length; col++) {
                    Label label;
                    Number number;
                    int parsedNum = 0;
                    boolean isNumber = true;
                    try {
                        parsedNum = Integer.parseInt(currentLine[col]);
                    } catch (NumberFormatException nfe) {
                        isNumber = false;
                    }
                    if (!isNumber) {
                        if (row == 0) {
                            label = new Label(col, row, currentLine[col]);
                            WritableFont cellFont = new WritableFont(WritableFont.ARIAL, 10);
                            cellFont.setBoldStyle(WritableFont.BOLD);
                            WritableCellFormat cellFormat = new WritableCellFormat(cellFont);
                            label = new Label(col, row, currentLine[col], cellFormat);
                        }
                        else {
                            label = new Label(col, row, currentLine[col]);
                        }

                        wsheet.addCell(label);
                    } else {
                        number = new Number(col, row, parsedNum);
                        wsheet.addCell(number);
                    }
                }
                row++;
            }

            wworkbook.write();
            wworkbook.close();


        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("[+] Excel file completed\n[+] Location:  " + excelFileURL);
        return excelFileURL;
    }

    public static void mail(String toEmail) {

        System.out.println("[+] Sending email to " + toEmail);

        final String username = "dontreplyinvoice@gmail.com";
        final String password = emailpass;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");


        Session session = Session.getInstance(props);

        try {

            Message message = new MimeMessage(session);
            message.setFrom((new InternetAddress(username))); // test, annars username
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject("Comparison services");

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("This is a comparison");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);

            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(excelFileURL);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName("Comparison.xls");
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);


            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", username, password);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

        } catch (AddressException ae) {
            System.out.println("Address exception, check recipient email");
        } catch (MessagingException me) {
            me.printStackTrace();
        }

        System.out.println("[+] Comparison successfully emailed to " + toEmail);
        System.out.println("[+] COMPARISON COMPLETE [+]\n---------------------------");
    }

}