package org.eclipse.leshan.client.demo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

public class CsvHandler {

    public static void writeDataLine(String filePath, String time, Float value) {

        // first create file object for file placed at location
        // specified by filepath
        File file = new File(filePath);
        FileWriter fileWriter = null;
        try {
            // create FileWriter object with file as parameter
            fileWriter = new FileWriter(file, true);
            fileWriter.append(time);
            fileWriter.append(',');
            fileWriter.append(value.toString());
            fileWriter.append('\n');
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }

    public static String readAllDataAtOnce(String file) { 
         
        FileReader filereader = null;
        CSVReader csvReader = null;
        try { 
            // Create an object of file reader 
            // class with CSV file as a parameter. 
            filereader = new FileReader(file); 
    
            // create csvReader object and skip first Line 
            csvReader = new CSVReaderBuilder(filereader).build(); 
            List<String[]> allData = csvReader.readAll(); 
            String payload = "";
            // print Data 
            for (String[] row : allData) { 
                payload += row[0]+","+row[1]+"\n";
            } 
            return payload;
        } 
        catch (IOException|CsvException e) { 
            e.printStackTrace(); 
            return "Nothing here";
        } finally {
            try {
                csvReader.close();
                filereader.close();
            } catch (IOException e) {
                e.printStackTrace(); 
            }
        }
    } 


    public static void rename_and_clear(String currentPath, String oldPath) { 
        Path currentFile = Paths.get(currentPath);
        Path oldFile = Paths.get(oldPath);
        System.out.println(currentFile);
        System.out.println(oldFile);

        try{

            // rename a file in the same directory
            Files.move(currentFile, oldFile, StandardCopyOption.REPLACE_EXISTING);
            FileWriter fw = new FileWriter(currentPath,false);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    } 
}