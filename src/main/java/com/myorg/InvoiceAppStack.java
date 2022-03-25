package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.EventType;
import software.amazon.awscdk.services.s3.notifications.SnsDestination;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;

public class InvoiceAppStack extends Stack {
    public InvoiceAppStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    private final Bucket bucket;
    private final Queue s3InvoiceQueue;

    public InvoiceAppStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        //Criando tópico SNS
        SnsTopic s3InvoiceTopic = SnsTopic.Builder.create(Topic.Builder.create(this, "S3InvoiceTopic")
                        .topicName("s3-invoice-events")
                        .build())
                .build();


        //Criando Bucket S3
        bucket = Bucket.Builder.create(this, "S301")
                .bucketName("pcs01-invoice")
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();


        //Adicionando um evento para toda vez que criar algum objeto no bucker, será notificado no tópico SNS
        bucket.addEventNotification(EventType.OBJECT_CREATED_PUT, new SnsDestination(s3InvoiceTopic.getTopic()));


        //Fila que representa a DLQ
        Queue s3InvoiceDlq = Queue.Builder.create(this, "S3InvoiceDlq")
                .queueName("s3-invoice-events-dlq")
                .build();
        //Criando a DLQ de fato com a partir da fila de cima.
        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                .queue(s3InvoiceDlq)
                .maxReceiveCount(3)
                .build();
        //Fila de fato que irá receber as informações
        s3InvoiceQueue = Queue.Builder.create(this, "S3InvoiceQueue")
                .queueName("s3-invoice-events")
                .deadLetterQueue(deadLetterQueue)
                .build();

        //Inscrevendo a fila no tópico SNS
        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(s3InvoiceQueue).build();
        s3InvoiceTopic.getTopic().addSubscription(sqsSubscription);
    }

    public Bucket getBucket() {
        return bucket;
    }

    public Queue getS3InvoiceQueue() {
        return s3InvoiceQueue;
    }
}

