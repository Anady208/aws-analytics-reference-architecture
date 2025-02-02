package com.geekoosh.flyway;

import com.adobe.testing.s3mock.S3MockRule;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.lambda.runtime.events.CloudFormationCustomResourceEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.geekoosh.flyway.request.FlywayMethod;
import com.geekoosh.flyway.response.Response;
import com.geekoosh.lambda.s3.S3Service;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FlywayCustomResourceHandlerTest extends TestCase {

    @Mock
    private AmazonCloudFormation cfnMock;


    @Test
    public void testHandleRequestForCreateCustomResourceTriggerFlyway() {
        // GIVEN
        String resourceLogicalId = "test";
        String physicalResourceId = "fakePhysicalResourceId";
        CloudFormationCustomResourceEvent event = new CloudFormationCustomResourceEvent();
        event.setRequestId("test");
        event.setLogicalResourceId(resourceLogicalId);
        event.setPhysicalResourceId(physicalResourceId);
        event.setRequestType("Create");

        FlywayCustomResourceHandler handler = new FlywayCustomResourceHandler();
        FlywayCustomResourceHandler handlerSpy = Mockito.spy(handler);

        Mockito.when(cfnMock.describeStackResource(new DescribeStackResourceRequest().withLogicalResourceId(resourceLogicalId))).thenReturn(new DescribeStackResourceResult().withStackResourceDetail(new StackResourceDetail().withResourceStatus("UPDATE_IN_PROGRESS")));
        Mockito.doReturn(null).when(handlerSpy).callFlywayService();

        // WHEN
        Map result = handlerSpy.handleRequest(event, cfnMock);


        // THEN
        assertEquals(result.get("PhysicalResourceId"), physicalResourceId);
        verify(handlerSpy, times(1)).callFlywayService();
    }

    @Test
    public void testHandleRequestForUpdateRollbackNotCallingFlywayService() {
        // GIVEN
        String resourceLogicalId = "test";
        String physicalResourceId = "fakePhysicalResourceId";
        CloudFormationCustomResourceEvent event = new CloudFormationCustomResourceEvent();
        event.setRequestId("test");
        event.setLogicalResourceId(resourceLogicalId);
        event.setPhysicalResourceId(physicalResourceId);
        event.setRequestType("Update");

        FlywayCustomResourceHandler handler = new FlywayCustomResourceHandler();
        FlywayCustomResourceHandler handlerSpy = Mockito.spy(handler);

        Mockito.when(cfnMock.describeStackResource(new DescribeStackResourceRequest().withLogicalResourceId(resourceLogicalId))).thenReturn(new DescribeStackResourceResult().withStackResourceDetail(new StackResourceDetail().withResourceStatus("UPDATE_ROLLBACK_IN_PROGRESS")));

        // WHEN
        Map result = handlerSpy.handleRequest(event, cfnMock);


        // THEN
        assertEquals(result.get("PhysicalResourceId"), physicalResourceId);
        verify(handlerSpy, times(0)).callFlywayService();
    }
}