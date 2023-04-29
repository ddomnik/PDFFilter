package org.dufner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.json.JSONArray;
import org.json.JSONObject;

import static java.lang.System.exit;
import static java.lang.System.out;

public class PDFFilter {

    private static String LOGFILE_PATH = "PDFFilter_log.txt";
    private static FileWriter LOGFILE = null;
    private static BufferedWriter LOGFILE_WRITER = null;

    private static String SETTINGS_DIR = "filter.json";
    private static String INPUT_DIR = "./";
    private static String NO_MATCH_DIR = "";
    private static String PROCESSING_ERROR_DIR = "";
    private static Boolean ADD_DATE = false;

    private static ArrayList<File> PDFs = new ArrayList<>();
    private static JSONArray FILTER;

    public static void main(String[] args) {

        out.println("Executing PDFFilter");

        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        Date date = new Date();
        System.out.println(dateFormat.format(date));

        try {
            LOGFILE = new FileWriter(LOGFILE_PATH, true);
            LOGFILE_WRITER = new BufferedWriter(LOGFILE);
            LOGFILE_WRITER.write("\n\nRUNNING SCRIPT "+dateFormat.format(date)+"\n");
            LOGFILE_WRITER.flush();
        } catch (IOException e) {
            out.println(e);
        }

        // Custom filter.json given
        if(args.length > 0){
            SETTINGS_DIR = args[0];
        }

        load_filter();
        load_pdfs();
        apply_filter();
    }

    public static void write_log(String log){
        out.println(log);
        try {
            LOGFILE_WRITER.write(log+"\n");
            LOGFILE_WRITER.flush();
        } catch (IOException e) {
            out.println(e);
        }
    }

    public static void run_script(String script_path, String parameter)
    {
        if(!script_path.contains(".ps1")){
            write_log("Only Windows Powershell scripts (.ps1) are currently supported");
            return;
        }

        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
        Process process;
        try {
            if (isWindows) {
                write_log("Run Windows script:");
                String str = String.format("powershell.exe &'%s' -pdf '%s'", script_path, parameter);
                write_log(str);
                process = Runtime.getRuntime().exec(str);
            } else {
                out.println("Run Linux script");
                LOGFILE_WRITER.write("Run Linux script");

                process = Runtime.getRuntime().exec(String.format("/bin/sh -c ls %s", "test"));
            }

            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(process.getErrorStream()));

            // Read the output from the command
            write_log("Output script:");
            String s = null;
            while ((s = stdInput.readLine()) != null) {
                write_log(s);
            }

            // Read any errors from the attempted command
            write_log("Output script ERROR:");
            while ((s = stdError.readLine()) != null) {
                write_log(s);
            }

        } catch (IOException e) {
            write_log(e.toString());
            exit(-1);
        }
    }

    public static void load_filter()
    {
        try {
            String json_file =  Files.readString(Path.of(SETTINGS_DIR));

            JSONObject json = new JSONObject(json_file);

            JSONObject read_settings = json.getJSONObject("settings");
            if(read_settings.has("folder"))
            {
                INPUT_DIR = read_settings.getString("folder");
            }

            if(read_settings.has("no_match"))
            {
                NO_MATCH_DIR = read_settings.getString("no_match");
            }

            if(read_settings.has("processing_error"))
            {
                PROCESSING_ERROR_DIR = read_settings.getString("processing_error");
            }

            FILTER = json.getJSONArray("filter");

            write_log("Loaded filters: "+FILTER.length());

        } catch (IOException e) {
            write_log(e.toString());
            exit(-1);
        }
    }

    public static void load_pdfs()
    {
        //Get all PDFs
        File folder = new File(INPUT_DIR);
        if(null != folder)
        {
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    if (file.getName().toLowerCase().endsWith(".pdf")) {
                        PDFs.add(file);
                    }
                }
            }
        }else {
            write_log("Folder with name " + INPUT_DIR + " not found!");
            exit(-1);
        }

        write_log("PDFs ("+PDFs.size()+"): "+PDFs.toString());
    }

    public static void apply_filter()
    {
            // Go through all PDFs
            if(PDFs.size() > 0) {
                for (int idx_pdf = 0; idx_pdf < PDFs.size(); idx_pdf++) {
                    try {
                        int matching_filter = -1;

                        write_log("Processing " + PDFs.get(idx_pdf).getName());

                        PdfReader pdf_reader = new PdfReader(PDFs.get(idx_pdf).getPath());
                        PdfDocument pdf = new PdfDocument(pdf_reader);

                        String pdf_date = "";

                        // Go through all pages
                        for (int idx_page = 1; idx_page <= pdf.getNumberOfPages(); idx_page++) {
                            String content = PdfTextExtractor.getTextFromPage(pdf.getPage(idx_page));

                            // Go through all filters
                            for (int idx_filter = 0; idx_filter < FILTER.length(); idx_filter++) {
                                JSONObject filter = FILTER.getJSONObject(idx_filter);

                                JSONArray keywords = new JSONArray();
                                if (filter.has("keywords")) {
                                    keywords.putAll(filter.getJSONArray("keywords"));

                                }

                                String regex = "";
                                if (filter.has("regex")) {
                                    regex = filter.getString("regex");

                                }

                                Boolean append_date = false;
                                if (filter.has("append_date")) {
                                    append_date = filter.getBoolean("append_date");
                                }


                                if (regex.equals("") && keywords.length() == 0) {
                                    write_log("filter " + idx_filter + " needs at least a 'keywords' (array) or 'regex' (String) entry.");
                                    idx_page = pdf.getNumberOfPages() + 1;
                                    continue;
                                }


                                // Search for keyword
                                for (int idx_keyword = 0; idx_keyword < keywords.length(); idx_keyword++) {
                                    if (content.contains(keywords.getString(idx_keyword))) {
                                        write_log("  Found keyword '" + keywords.getString(idx_keyword) + "' in PDF '" + PDFs.get(idx_pdf).getName() + "'");
                                        matching_filter = idx_filter;
                                        break;
                                    }
                                }

                                // Search for regex
                                if (!regex.equals("")) {
                                    Pattern pattern = Pattern.compile(regex);
                                    Matcher matcher = pattern.matcher(content);
                                    if (matcher.find()) {
                                        write_log("  Regex '" + regex + "' matches content of PDF '" + PDFs.get(idx_pdf).getName() + "'");
                                        matching_filter = idx_filter;
                                        break;
                                    }
                                }

                                //Search for date (extra)
                                if (append_date) {
                                    Pattern pattern = Pattern.compile("(0[1-9]|[12][0-9]|3[01])[-.](0[1-9]|1[012])[-.](20)\\d\\d");
                                    Matcher matcher = pattern.matcher(content);
                                    if (matcher.find()) {
                                        pdf_date = matcher.group();
                                        write_log("  Date 'pdf_date' found in PDF '" + PDFs.get(idx_pdf).getName() + "'");
                                    }
                                }


                                if (matching_filter != -1) {
                                    break;
                                }
                            }
                            if (matching_filter != -1) {
                                break;
                            }
                        }

                        pdf.close();
                        pdf_reader.close();

                        if (matching_filter != -1) //A filter matched
                        {
                            JSONObject filter = FILTER.getJSONObject(matching_filter);

                            String move = "";
                            if (filter.has("move_to")) {
                                move = filter.getString("move_to");
                            }

                            String script = "";
                            if (filter.has("run_script")) {
                                script = filter.getString("run_script");
                            }


                            if (move.equals("") && script.equals("")) {
                                write_log("filter " + matching_filter + " needs at least a 'move_to' (String) or 'run_script' (String) entry.");
                                continue;
                            }


                            if (!move.equals("")) {
                                if (!pdf_date.equals("")) {
                                    int idx_file_dot = PDFs.get(idx_pdf).getName().lastIndexOf(".");
                                    String filename = PDFs.get(idx_pdf).getName().substring(0, idx_file_dot);
                                    String fileext = PDFs.get(idx_pdf).getName().substring(idx_file_dot);
                                    write_log("  File moved (match): " + PDFs.get(idx_pdf).renameTo(new File(move + "/" + filename + " " + pdf_date + fileext)));
                                } else {
                                    write_log("  File moved (match): " + PDFs.get(idx_pdf).renameTo(new File(move + "/" + PDFs.get(idx_pdf).getName())));
                                }
                            }

                            if (!script.equals("")) {
                                run_script(script, PDFs.get(idx_pdf).getAbsolutePath());
                            }

                        } else //No filter matched
                        {
                            if (!NO_MATCH_DIR.equals("")) {
                                if (!pdf_date.equals("")) {
                                    int idx_file_dot = PDFs.get(idx_pdf).getName().lastIndexOf(".");
                                    String filename = PDFs.get(idx_pdf).getName().substring(0, idx_file_dot);
                                    String fileext = PDFs.get(idx_pdf).getName().substring(idx_file_dot);
                                    write_log("  File moved (no match): " + PDFs.get(idx_pdf).renameTo(new File(NO_MATCH_DIR + "/" + filename + " " + pdf_date + fileext)));
                                } else {
                                    write_log("  File moved (no match): " + PDFs.get(idx_pdf).renameTo(new File(NO_MATCH_DIR + "/" + PDFs.get(idx_pdf).getName())));
                                }
                            }
                        }
                    }
                    catch(Exception e) {

                        System.gc();

                        try { //Give garbage collector time to release opened files
                            Thread.sleep(100);
                        } catch (InterruptedException er) {
                            Thread.currentThread().interrupt();
                        }
                        write_log("  ERROR: "+e.toString());
                        write_log("  File moved (error): " + PDFs.get(idx_pdf).renameTo(new File(PROCESSING_ERROR_DIR + "/" + PDFs.get(idx_pdf).getName())));
                    }
                }
            }
    }
}
