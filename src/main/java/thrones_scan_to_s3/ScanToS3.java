package thrones_scan_to_s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by oliverl1
 */
public class ScanToS3 {


    private static final int numCharacters=42;
    private static final int numOrganizations=15;
    private static final int numLocations=32;
    private static final int numEvents=10;
    private static final int numEpisodes=40;

    private static final String siteDomain ="https://www.google.com";


    private static final String bucketName     = "valar-morghulis.org";

    private static AmazonS3 s3client;


    //test main
    public static void main(String[] args) {

        RestTemplate restTemplate = new RestTemplate();



        String test=restTemplate.getForObject(siteDomain,String.class);

        //System.out.println(test);


        if (initS3Client()) return;

        if(!s3client.doesBucketExist(bucketName)){
            s3client.createBucket(bucketName);
        }



        uploadToS3("test",test);



    }


    public static void realmain(String[] args) {

        if (initS3Client()) return;

        if(!s3client.doesBucketExist(bucketName)){
            s3client.createBucket(bucketName);
        }


        RestTemplate restTemplate = new RestTemplate();


        uploadToS3("main",restTemplate.getForObject(siteDomain,String.class));

        uploadToS3("characters",restTemplate.getForObject(siteDomain +"/characters",String.class));
        uploadToS3("organizations",restTemplate.getForObject(siteDomain +"/organizations",String.class));
        uploadToS3("locations",restTemplate.getForObject(siteDomain +"/locations",String.class));
        uploadToS3("events",restTemplate.getForObject(siteDomain +"/events",String.class));
        uploadToS3("episodes",restTemplate.getForObject(siteDomain +"/episodes",String.class));
        uploadToS3("about",restTemplate.getForObject(siteDomain +"/about",String.class));


        new Thread(() -> fetchPageAndUpload("characters",numCharacters)).start();
        new Thread(() -> fetchPageAndUpload("organizations",numOrganizations)).start();
        new Thread(() -> fetchPageAndUpload("locations",numLocations)).start();
        new Thread(() -> fetchPageAndUpload("events",numEvents)).start();
        new Thread(() -> fetchPageAndUpload("episodes",numEpisodes)).start();









    }

    private static boolean initS3Client() {
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


        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return true;
        }


        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretKeyId);
        s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
        return false;
    }




    private static void fetchPageAndUpload(String path,int numPages) {

        RestTemplate restTemplate = new RestTemplate();

        for(int n=1;n<=numPages;n++){

            final String keyName=path+"/"+ n;

            new Thread(()->uploadToS3(keyName,restTemplate.getForObject(siteDomain +"/"+keyName,String.class))).start();

        }

    }





    private static void uploadToS3(String keyName,String uploadObject){


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
