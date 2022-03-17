package com.myorg;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.rds.*;

import java.util.Collections;

public class RdsStack extends Stack {
    public RdsStack(final Construct scope, final String id, Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public RdsStack(final Construct scope, final String id, final StackProps props, Vpc vpc) {
        super(scope, id, props);


        //Conseguir passar a senha como parâmetro
        CfnParameter databasePassword = CfnParameter.Builder.create(this, "databasePassword")
                .type("String")
                .description("The RDS instance password")
                .build();


        //Buscar security group da VPC e adicionando nova regra de entrada para a porta 3306
        //Mesmo definindo "Peer.anyIpv4()" apenas dispositivos dentro da VPC conseguem acessar essa porta para o RDS.
        ISecurityGroup iSecurityGroup = SecurityGroup.fromSecurityGroupId(this, id, vpc.getVpcDefaultSecurityGroup());
        iSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));


        //Criação da instância RDS
        DatabaseInstance databaseInstance = DatabaseInstance.Builder
                .create(this, "Rds01")
                .instanceIdentifier("aws-project001-db")
                //.publiclyAccessible(true) esse parâmetro é para conseguir acessar ele pelo cliente mysql, por exemplo.. mas não é bom ter o banco público assim.
                .engine(DatabaseInstanceEngine.mysql(MySqlInstanceEngineProps.builder()
                        .version(MysqlEngineVersion.VER_8_0_26)
                        .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername("admin",
                        CredentialsFromUsernameOptions.builder()
                                .password(SecretValue.plainText(databasePassword.getValueAsString()))
                                .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(iSecurityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPublicSubnets())
                        .build())
                .build();


        //Exportar parâmetros para conseguir ler em outra stack
        //Parâmetro do endpoint para acessar o banco
        CfnOutput.Builder.create(this, "rds-endpoint")
                .exportName("rds-endpoint")
                .value(databaseInstance.getDbInstanceEndpointAddress())
                .build();

        //Parâmetro da senha para acessar o banco
        CfnOutput.Builder.create(this, "rds-password")
                .exportName("rds-password")
                .value(databasePassword.getValueAsString())
                .build();

    }
}
