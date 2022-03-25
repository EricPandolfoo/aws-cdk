package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.DeadLetterQueue;
import software.amazon.awscdk.services.sqs.Queue;

import java.util.HashMap;
import java.util.Map;

public class Service02Stack extends Stack {

    public Service02Stack(final Construct scope, final String id, Cluster cluster, SnsTopic productEventsTopic,
                          Table productEventDdb) {
        this(scope, id, null, cluster, productEventsTopic, productEventDdb);
    }

    public Service02Stack(final Construct scope, final String id, final StackProps props, Cluster cluster,
                          SnsTopic productEventsTopic, Table productEventDdb) {
        super(scope, id, props);


        //Construcao da fila "comum" que vai se tornar uma DLQ - Dead Letter Queue
        Queue productEventsDlq = Queue.Builder.create(this, "ProductEventsDlq")
                .queueName("product-events-dlq")
                .build();
        //Criando a DLQ
        DeadLetterQueue deadLetterQueue = DeadLetterQueue.builder()
                .queue(productEventsDlq)
                .maxReceiveCount(3)
                .build();
        //Fila principal
        Queue productEventsQueue = Queue.Builder.create(this, "ProductEvents")
                .queueName("product-events")
                .deadLetterQueue(deadLetterQueue)
                .build();


        //Inscrevendo a fila no tópico SNS criado (trigger).
        SqsSubscription sqsSubscription = SqsSubscription.Builder.create(productEventsQueue).build();
        productEventsTopic.getTopic().addSubscription(sqsSubscription);


        //Variáveis de ambiente que vão estar no container para acesso ao banco de dados RDS
        //Essas variáveis vão ser passadas para a aplicação para conseguir acessar o banco
        // (foram criadas as variáveis também no application.properties da aplicação, projeto aws_project01)
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("AWS_REGION", "us-east-1");
        envVariables.put("AWS_SQS_QUEUE_PRODUCT_EVENTS_NAME", productEventsQueue.getQueueName());


        //Criação do container + imagem do container que está no DockerHub
        ApplicationLoadBalancedFargateService service02 = ApplicationLoadBalancedFargateService.Builder
                .create(this, "ALB02")
                .serviceName("service02")
                .cluster(cluster)
                .memoryLimitMiB(1024)
                .cpu(512)
                .desiredCount(1)
                .listenerPort(9090)
                .assignPublicIp(true)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("aws_project")
                                .image(ContainerImage.fromRegistry("pandolfo/curso_aws_microservice02:1.2.0"))
                                .containerPort(9090)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "Service02LogGroup")
                                                .logGroupName("Service02")
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix("Service02")
                                        .build()))
                                .environment(envVariables)
                                .build())
                .publicLoadBalancer(true)
                .build();


        //Configuração do endpoint para verificação do Status das instâncias (foi necessário importar a dependência do "actuator" no container Docker)
        service02.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("9090")
                .healthyHttpCodes("200")
                .build());

        //Auto Scaling com os parâmetros de capacidade mínima máxima
        ScalableTaskCount scalableTaskCount = service02.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(1)
                .maxCapacity(2)
                .build());

        //Porcentagem de "target" para fazer o scale-out e scale-in, bem como, a quantidade de tempo necessário para
        // ficar a cima ou a baixo desse limite para provicionar ou remover uma instância
        scalableTaskCount.scaleOnCpuUtilization("Service02AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(80)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());


        //Permissão para a task ecs do serviço02 consumir a fila sqs
        productEventsQueue.grantConsumeMessages(service02.getTaskDefinition().getTaskRole());

        //Permissão para a task ecs do serviço02 de leitura e escrita no DynamoDB
        productEventDdb.grantReadWriteData(service02.getTaskDefinition().getTaskRole());


    }
}
