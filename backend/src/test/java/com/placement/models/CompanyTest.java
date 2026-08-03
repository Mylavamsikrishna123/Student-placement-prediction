package com.placement.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Company Model Tests")
public class CompanyTest {
    private Company company;

    @BeforeEach
    public void setUp() {
        company = new Company();
    }

    @Test
    @DisplayName("Test Default Constructor")
    public void testDefaultConstructor() {
        assertNotNull(company);
    }

    @Test
    @DisplayName("Test Company Name Setter and Getter")
    public void testCompanyNameSetterGetter() {
        company.setName("Google");
        assertEquals("Google", company.getName());
    }

    @Test
    @DisplayName("Test Company ID Setter and Getter")
    public void testCompanyIdSetterGetter() {
        company.setId(1);
        assertEquals(1, company.getId());
    }

    @Test
    @DisplayName("Test Multiple Companies with Different Names")
    public void testMultipleCompanies() {
        Company company1 = new Company();
        Company company2 = new Company();
        
        company1.setName("Microsoft");
        company2.setName("Apple");
        
        assertEquals("Microsoft", company1.getName());
        assertEquals("Apple", company2.getName());
        assertNotEquals(company1.getName(), company2.getName());
    }

    @Test
    @DisplayName("Test Company with TCS")
    public void testTCSCompany() {
        company.setId(1);
        company.setName("TCS");
        
        assertEquals(1, company.getId());
        assertEquals("TCS", company.getName());
    }

    @Test
    @DisplayName("Test Company with Infosys")
    public void testInfosysCompany() {
        company.setId(2);
        company.setName("Infosys");
        
        assertEquals(2, company.getId());
        assertEquals("Infosys", company.getName());
    }

    @Test
    @DisplayName("Test Company with Amazon")
    public void testAmazonCompany() {
        company.setId(3);
        company.setName("Amazon");
        
        assertEquals(3, company.getId());
        assertEquals("Amazon", company.getName());
    }

    @Test
    @DisplayName("Test Company Name Not Null")
    public void testCompanyNameNotNull() {
        company.setName("IBM");
        assertNotNull(company.getName());
    }

    @Test
    @DisplayName("Test Company ID Positive Value")
    public void testCompanyIdPositive() {
        company.setId(100);
        assertTrue(company.getId() > 0);
    }

    @Test
    @DisplayName("Test Company Name Update")
    public void testCompanyNameUpdate() {
        company.setName("Oracle");
        assertEquals("Oracle", company.getName());
        
        company.setName("Salesforce");
        assertEquals("Salesforce", company.getName());
    }

    @Test
    @DisplayName("Test Company Set and Get ID Multiple Times")
    public void testCompanyIdMultipleTimes() {
        company.setId(5);
        assertEquals(5, company.getId());
        
        company.setId(10);
        assertEquals(10, company.getId());
        
        company.setId(15);
        assertEquals(15, company.getId());
    }
}
