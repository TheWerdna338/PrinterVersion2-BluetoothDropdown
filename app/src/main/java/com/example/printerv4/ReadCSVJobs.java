package com.example.printerv4;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class ReadCSVJobs extends Thread {

    String projectName;
    String spreadsheetID;
    ArrayList<String> projectNames = new ArrayList<String>();
    ArrayList<String> spreadsheetIDs = new ArrayList<String>();

    ArrayList<String> lines = new ArrayList<String>(); //create an arraylist that contains all the individual lines of the csv

    @Override
    public void run() {
        try {
            URL url = new URL("https://docs.google.com/spreadsheets/d/1Hz21fhh_VL0bJ8Ivf3Fw9sLL--XQaNSXxlXZTem5jUc/gviz/tq?tqx=out:csv&sheet=Sheet1");
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(String.valueOf(url));
            HttpResponse response = httpClient.execute(httpGet, localContext);
            InputStream is = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            reader.readLine(); //this will read the first line (which contains the field names of the CSV file which we dont need)
            String line=null; //declare 1st line as null so loop doesnt pull any information from it

            while ((line = reader.readLine()) != null) { //loop doesnt get any info from 1st line and thus runs from 2nd line
                lines.add(line);//add an entire individual line the the arraylist, so the lines arraylist will have every single line of the job database in it
                                //this will be returned into the main activity such that when you select a job it reads the position of selection, and then fetches the desired line from the lines arraylist

                String[] RowData = line.split(",");
                projectName = RowData[0].replace("\"",""); //we need to use replace as if not everything read from the CSV will be surrounded with quotation marks, e.g. Engineers will come out as "Engineers"
                spreadsheetID = RowData[1].replace("\"","");//this essentially searches for any quotation (\" meaning any quotation mark, with \ literally meaning 'any' in this case)) within the string and replaces it with nothing ("")
                // do something with "projectName" and "spreadsheetID"
                projectNames.add(projectName);
                spreadsheetIDs.add(spreadsheetID);

            }

            is.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getProjectNames(){
        return projectNames;
    }
    public ArrayList<String>  getSpreadsheetIDs(){
        return spreadsheetIDs;
    }

    public ArrayList<String> getLines(){
        return lines;
    }
}
