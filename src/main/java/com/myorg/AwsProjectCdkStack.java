package com.myorg;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class AwsProjectCdkStack extends Stack {
    public AwsProjectCdkStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public AwsProjectCdkStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


    }
}
