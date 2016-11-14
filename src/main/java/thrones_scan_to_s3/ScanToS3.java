package thrones_scan_to_s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by oliverl1
 */
public class ScanToS3 {


    private static final int numCharacters=42;
    private static final int numOrganizations=15;
    private static final int numLocations=32;
    private static final int numEvents=10;
    private static final int numEpisodes=40;

    private static final String siteDomain ="http://valar-morghulis.org/";


    private static final String bucketName = "valar-morghulis.org";

    //private static AmazonS3 s3client;


    //test main
    public static void main(String[] args) {

        String test=simpleGet(siteDomain);
        //System.out.println(test);


        final AmazonS3 s3client;
        try {
            s3client = initS3Client();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("failed to create s3 client");
            return;
        }

        System.out.println("S3 CLIENT CREATED");

        if(!s3client.doesBucketExist(bucketName)){
            System.out.println("NO BUCKET, CREATING");
            s3client.createBucket(bucketName);
        }
        else{
            System.out.println("BUCKET ALREADY EXISTS");
        }

        System.out.println("OK BUCKET");



        uploadToS3(s3client,"test",test);



    }


    public static void realmain(String[] args) {

        final AmazonS3 s3client;
        try {
            s3client = initS3Client();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("failed to create s3 client");
            return;
        }

        if(!s3client.doesBucketExist(bucketName)){
            s3client.createBucket(bucketName);
        }


        uploadToS3(s3client,"main",simpleGet(siteDomain));

        uploadToS3(s3client,"characters",simpleGet(siteDomain+"/characters"));
        uploadToS3(s3client,"organizations",simpleGet(siteDomain +"/organizations"));
        uploadToS3(s3client,"locations",simpleGet(siteDomain +"/locations"));
        uploadToS3(s3client,"events",simpleGet(siteDomain +"/events"));
        uploadToS3(s3client,"episodes",simpleGet(siteDomain +"/episodes"));
        uploadToS3(s3client,"about",simpleGet(siteDomain +"/about"));


        new Thread(() -> fetchPageAndUpload(s3client,"characters",numCharacters)).start();
        new Thread(() -> fetchPageAndUpload(s3client,"organizations",numOrganizations)).start();
        new Thread(() -> fetchPageAndUpload(s3client,"locations",numLocations)).start();
        new Thread(() -> fetchPageAndUpload(s3client,"events",numEvents)).start();
        new Thread(() -> fetchPageAndUpload(s3client,"episodes",numEpisodes)).start();
    }



    private static AmazonS3 initS3Client() throws Exception{
        String accessKeyId;
        String secretKeyId;

        try {


            BufferedReader br = new BufferedReader(new FileReader("./dev_user.txt"));



            //skip the first line
            br.readLine();

            String[] line=br.readLine().split(",");
            System.out.println("line: "+line[0]);
            //in my current file format, the needed fields are the 2nd and 3rd entry in the csv file
            accessKeyId=line[1];
            secretKeyId=line[2];


        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("failed to read cred strings");
            //return null;
            throw new Exception();
        }


        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretKeyId);
        AmazonS3 s3client= new AmazonS3Client(awsCreds);
        s3client.setRegion(com.amazonaws.regions.Region.getRegion(Regions.US_EAST_1));
        return s3client;

    }




    private static void fetchPageAndUpload(AmazonS3 s3client, String category,int numPages) {


        for(int n=1;n<=numPages;n++){

            final String keyName=category+"/"+ n;

            new Thread(()->uploadToS3(s3client,keyName,simpleGet(siteDomain+"/"+keyName))).start();

        }

    }


    private static String simpleGet(String url){

        RestTemplate restTemplate = new RestTemplate();


        return restTemplate.getForObject(url,String.class);
    }





    private static void uploadToS3(AmazonS3 s3client,String keyName,String uploadObject){


        try {
            System.out.println("Uploading a new object to S3 from a file\n");

            s3client.putObject(new PutObjectRequest(bucketName, keyName, uploadObject));

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        }
    }


}
