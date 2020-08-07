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

public class ReadCSVSample extends Thread{

    ArrayList<String> sampleRows = new ArrayList<>();
    String spreadsheetID;
    String boreholeName;

    String sampleNo;
    ArrayList<String> sampleNos = new ArrayList<>();

    String sampleType;
    ArrayList<String> sampleTypes = new ArrayList<>();

    ReadCSVSample(String spreadsheetID, String boreholeName){
        this.spreadsheetID = spreadsheetID;
        this.boreholeName = boreholeName;
    }


    @Override
    public void run() {
        try {
            URL url = new URL("https://docs.google.com/spreadsheets/d/"+spreadsheetID+"/gviz/tq?tqx=out:csv&sheet="+boreholeName);
            HttpClient httpClient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();
            HttpGet httpGet = new HttpGet(String.valueOf(url));
            HttpResponse response = httpClient.execute(httpGet, localContext);
            InputStream is = response.getEntity().getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            reader.readLine(); //this will read the first line (which contains the field names of the CSV file which we dont need)
            String line=null; //declare 1st line as null so loop doesnt pull any information from it


            while ((line = reader.readLine()) != null) { //loop doesnt get any info from 1st line and thus runs from 2nd line
                sampleRows.add(line);//add an entire individual line the the arraylist, so the lines arraylist will have every single line of the samples in the desired sample sheet

                String[] RowData = line.split(",");

                sampleNo =RowData[1].replace("\"", "");
                sampleNos.add(sampleNo);

                sampleType =RowData[3].replace("\"", "");
                sampleTypes.add(sampleType);
            }

            is.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> getSampleNos() {
        return sampleNos;
    }

    public ArrayList<String> getSampleRows(){
        return sampleRows;
    }

    public ArrayList<String> getSampleTypes(){
        return sampleTypes;
    }
}
