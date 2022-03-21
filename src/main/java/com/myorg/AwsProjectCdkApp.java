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

        //Service01
        Service01Stack service01Stack = new Service01Stack(app, "Service01",
                clusterStack.getCluster(), snsStack.getProductEventsTopic());
        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);

        //Service02
        Service02Stack service02Stack = new Service02Stack(app, "Service02",
                clusterStack.getCluster(), snsStack.getProductEventsTopic());
        service02Stack.addDependency(clusterStack);
        service02Stack.addDependency(snsStack);

        app.synth();
    }
}
