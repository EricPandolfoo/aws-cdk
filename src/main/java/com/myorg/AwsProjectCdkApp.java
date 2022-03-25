package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;

public class AwsProjectCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        //VPC
       VpcStack vpcStack = new VpcStack(app, "Vpc");


       //Cluster do ECS
        ClusterStack clusterStack = new ClusterStack(app, "Cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        //RDS
        RdsStack rdsStack = new RdsStack(app, "Rds", vpcStack.getVpc());
        rdsStack.addDependency(vpcStack);

        //SNS TOPIC
        SnsStack snsStack = new SnsStack(app, "Sns");

        //InvoiceApp
        InvoiceAppStack invoiceAppStack = new InvoiceAppStack(app,"InvoiceApp");


        //Service01
        Service01Stack service01Stack = new Service01Stack(app, "Service01",
                clusterStack.getCluster(), snsStack.getProductEventsTopic(), invoiceAppStack.getBucket(), invoiceAppStack.getS3InvoiceQueue());
        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);
        service01Stack.addDependency(invoiceAppStack);

        //DynamoDB
        DynamoDBStack dynamoDBStack = new DynamoDBStack(app, "DynamoDB");


        //Service02
        Service02Stack service02Stack = new Service02Stack(app, "Service02",
                clusterStack.getCluster(), snsStack.getProductEventsTopic(), dynamoDBStack.getProductEventsDdb());
        service02Stack.addDependency(clusterStack);
        service02Stack.addDependency(snsStack);
        service02Stack.addDependency(dynamoDBStack);

        app.synth();
    }
}
