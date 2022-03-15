package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;

public class Service01Stack extends Stack {
    public Service01Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service01Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);

        //Criação do container + imagem do container que está no DockerHub
        ApplicationLoadBalancedFargateService service01 = ApplicationLoadBalancedFargateService.Builder
                .create(this, "ALB01")
                .serviceName("service01")
                .cluster(cluster)
                .memoryLimitMiB(1024)
                .cpu(512)
                .desiredCount(1)
                .listenerPort(8080)
                .assignPublicIp(true)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("aws_project")
                                .image(ContainerImage.fromRegistry("pandolfo/curso_aws_microservice01:1.0.0"))
                                .containerPort(8080)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                        .logGroup(LogGroup.Builder.create(this, "Service01LogGroup")
                                                .logGroupName("Service01")
                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                .build())
                                        .streamPrefix("Service01")
                                        .build()))
                                .build())
                .publicLoadBalancer(true)
                .build();


        //Configuração do endpoint para verificação do Status da aplicação (foi necessário importar a dependência do "actuator" no container Docker)
        service01.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("8080")
                .healthyHttpCodes("200")
                .build());

        //Auto Scaling com os parâmetros de capacidade mínima máxima
        ScalableTaskCount scalableTaskCount = service01.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(1)
                .maxCapacity(2)
                .build());

        //Porcentagem de "target" para fazer o scale-out e scale-in, bem como, a quantidade de tempo necessário para
        // ficar a cima ou a baixo desse limite para provicionar ou remover uma instância
        scalableTaskCount.scaleOnCpuUtilization("Service01AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(80)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

    }
}
