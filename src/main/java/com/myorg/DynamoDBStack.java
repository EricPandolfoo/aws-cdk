package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.dynamodb.*;

public class DynamoDBStack extends Stack {

    private final Table productEventsDdb;

    public DynamoDBStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamoDBStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        //Configurações do DynamoDB
        productEventsDdb = Table.Builder.create(this, "ProductEventsDb")
                .tableName("product-events")
                .billingMode(BillingMode.PROVISIONED)
                .readCapacity(1)
                .writeCapacity(1)
                .partitionKey(Attribute.builder()
                        .name("pk")
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();


        //Configuração de AutoScalling para o DynamoDB
        /*productEventsDdb.autoScaleReadCapacity(EnableScalingProps.builder()
                        .minCapacity(1)
                        .maxCapacity(4)
                        .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());

        productEventsDdb.autoScaleWriteCapacity(EnableScalingProps.builder()
                        .minCapacity(1)
                        .maxCapacity(4)
                        .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());*/
    }

    public Table getProductEventsDdb() {
        return productEventsDdb;
    }
}
