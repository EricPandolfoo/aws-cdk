package com.myorg;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Arrays;

public class AwsProjectCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        //VPC
       VpcStack vpc = new VpcStack(app, "Vpc");


       //Cluster do ECS
        ClusterStack cluster = new ClusterStack(app, "Cluster", vpc.getVpc());
        cluster.addDependency(vpc);

        //Service01
        Service01Stack service01Stack = new Service01Stack(app, "Service01", cluster.getCluster());
        service01Stack.addDependency(cluster);

        app.synth();
    }
}
