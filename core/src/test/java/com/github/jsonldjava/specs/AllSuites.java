package com.github.jsonldjava.specs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ JsonLdApiSuite.class, JsonLdFramingSuite.class, JsonLd1Tests.class })
public class AllSuites {

}
