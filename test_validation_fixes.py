#!/usr/bin/env python3
"""
Test script to verify all validation fixes
Tests the backend API endpoints for the 5 fixed validation issues
"""

import requests
import json
import time

BASE_URL = "http://127.0.0.1:8080/api"
HEADERS = {"Content-Type": "application/json"}

# Test data
TEST_EMAIL = "testvalidation@example.com"
ADMIN_EMAIL = "admin@example.com"
ADMIN_PASS = "admin123"

def print_test(test_name, result, message=""):
    status = "✓ PASS" if result else "✗ FAIL"
    print(f"{status}: {test_name}")
    if message:
        print(f"  └─ {message}")

def health_check():
    """Check if server is running"""
    try:
        resp = requests.get(f"{BASE_URL}/health", timeout=5)
        print("✅ Server is running on port 8080\n")
        return True
    except Exception as e:
        print(f"❌ Server is not responding: {e}")
        return False

def test_registration(email, password):
    """Register a test user"""
    try:
        resp = requests.post(f"{BASE_URL}/register", 
            json={"email": email, "password": password},
            headers=HEADERS, timeout=5)
        return resp.status_code == 200
    except:
        return False

def test_login(email, password, role="student"):
    """Test login with credentials"""
    try:
        resp = requests.post(f"{BASE_URL}/login",
            json={"email": email, "password": password, "role": role},
            headers=HEADERS, timeout=5)
        return resp.status_code == 200
    except Exception as e:
        print(f"Error: {e}")
        return False

def test_name_validation():
    """Test Fix #1: Name field validation"""
    print("\n" + "="*60)
    print("TEST FIX #1: Name Field Validation")
    print("="*60)
    
    email = "nametest@example.com"
    test_registration(email, "password123")
    
    test_cases = [
        ("John Smith", True, "Valid name with letters and space"),
        ("Mary-Jane", True, "Valid name with hyphen"),
        ("O'Connor", True, "Valid name with apostrophe"),
        ("John123", False, "Invalid: contains numbers"),
        ("@name", False, "Invalid: contains special characters"),
        ("john@", False, "Invalid: contains @ symbol"),
        ("", False, "Invalid: empty name"),
    ]
    
    passed = 0
    for name, should_pass, description in test_cases:
        resp = requests.post(f"{BASE_URL}/student/profile",
            json={
                "email": email,
                "name": name,
                "idNumber": "STU123456",
                "department": "CSE",
                "degree": "B.Tech",
                "collegeName": "ABC College",
                "phone": "9876543210",
                "cgpa": 7.5,
                "branch": "CSE",
                "certifications": "",
                "backlogs": 0,
                "skills": {}
            },
            headers=HEADERS, timeout=5)
        
        is_valid = (resp.status_code == 200)
        is_correct = (is_valid == should_pass)
        
        if is_correct:
            passed += 1
            symbol = "✓"
        else:
            symbol = "✗"
        
        status = "accepted" if is_valid else "rejected"
        expected = "should be accepted" if should_pass else "should be rejected"
        print(f"{symbol} {description}: {status} ({expected})")
    
    print(f"\nName Validation: {passed}/{len(test_cases)} tests passed")
    return passed == len(test_cases)

def test_phone_validation():
    """Test Fix #2: Phone number validation"""
    print("\n" + "="*60)
    print("TEST FIX #2: Phone Number Validation")
    print("="*60)
    
    email = "phonetest@example.com"
    test_registration(email, "password123")
    
    test_cases = [
        ("9876543210", True, "Valid: 10 digits"),
        ("987654321012345", True, "Valid: 15 digits"),
        ("+919876543210", False, "Invalid: has + sign"),
        ("123456", False, "Invalid: less than 10 digits"),
        ("12345678901234567", False, "Invalid: more than 15 digits"),
        ("abcd123456", False, "Invalid: contains letters"),
        ("", False, "Invalid: empty phone"),
    ]
    
    passed = 0
    for phone, should_pass, description in test_cases:
        resp = requests.post(f"{BASE_URL}/student/profile",
            json={
                "email": email,
                "name": "Phone Test",
                "idNumber": "STU654321",
                "department": "ECE",
                "degree": "B.Tech",
                "collegeName": "XYZ College",
                "phone": phone,
                "cgpa": 8.0,
                "branch": "ECE",
                "certifications": "",
                "backlogs": 0,
                "skills": {}
            },
            headers=HEADERS, timeout=5)
        
        is_valid = (resp.status_code == 200)
        is_correct = (is_valid == should_pass)
        
        if is_correct:
            passed += 1
            symbol = "✓"
        else:
            symbol = "✗"
        
        status = "accepted" if is_valid else "rejected"
        expected = "should be accepted" if should_pass else "should be rejected"
        print(f"{symbol} {description}: {status} ({expected})")
    
    print(f"\nPhone Validation: {passed}/{len(test_cases)} tests passed")
    return passed == len(test_cases)

def test_cgpa_validation():
    """Test Fix #3: CGPA range validation"""
    print("\n" + "="*60)
    print("TEST FIX #3: CGPA Range Validation (0-10)")
    print("="*60)
    
    email = "cgpatest@example.com"
    test_registration(email, "password123")
    
    test_cases = [
        (0.0, True, "Valid: CGPA = 0.0"),
        (5.5, True, "Valid: CGPA = 5.5 (middle range)"),
        (10.0, True, "Valid: CGPA = 10.0"),
        (-1.0, False, "Invalid: CGPA = -1.0 (below range)"),
        (10.01, False, "Invalid: CGPA = 10.01 (above range)"),
        (11.0, False, "Invalid: CGPA = 11.0 (way above range)"),
    ]
    
    passed = 0
    for cgpa, should_pass, description in test_cases:
        resp = requests.post(f"{BASE_URL}/student/profile",
            json={
                "email": email,
                "name": "CGPA Test",
                "idNumber": "STU111111",
                "department": "ME",
                "degree": "B.Tech",
                "collegeName": "PQR College",
                "phone": "9111111111",
                "cgpa": cgpa,
                "branch": "Mechanical",
                "certifications": "",
                "backlogs": 0,
                "skills": {}
            },
            headers=HEADERS, timeout=5)
        
        is_valid = (resp.status_code == 200)
        is_correct = (is_valid == should_pass)
        
        if is_correct:
            passed += 1
            symbol = "✓"
        else:
            symbol = "✗"
        
        status = "accepted" if is_valid else "rejected"
        expected = "should be accepted" if should_pass else "should be rejected"
        print(f"{symbol} {description}: {status} ({expected})")
    
    print(f"\nCGPA Validation: {passed}/{len(test_cases)} tests passed")
    return passed == len(test_cases)

def test_email_case_insensitivity():
    """Test Fix #4: Email case-insensitivity"""
    print("\n" + "="*60)
    print("TEST FIX #4: Email Case-Insensitivity in Login")
    print("="*60)
    
    email = "CaseTest@Example.Com"
    password = "testpass123"
    
    # Register with mixed case email
    reg_resp = requests.post(f"{BASE_URL}/register",
        json={"email": email, "password": password},
        headers=HEADERS, timeout=5)
    
    test_cases = [
        ("CaseTest@Example.Com", True, "Original case"),
        ("casetest@example.com", True, "Lowercase"),
        ("CASETEST@EXAMPLE.COM", True, "Uppercase"),
        ("CaseTeSt@ExAmPlE.cOm", True, "Mixed case"),
        ("wrongemail@example.com", False, "Wrong email"),
    ]
    
    passed = 0
    for login_email, should_pass, description in test_cases:
        resp = requests.post(f"{BASE_URL}/login",
            json={"email": login_email, "password": password, "role": "student"},
            headers=HEADERS, timeout=5)
        
        is_valid = (resp.status_code == 200)
        is_correct = (is_valid == should_pass)
        
        if is_correct:
            passed += 1
            symbol = "✓"
        else:
            symbol = "✗"
        
        status = "login success" if is_valid else "login failed"
        expected = "should succeed" if should_pass else "should fail"
        print(f"{symbol} {description}: {status} ({expected})")
    
    print(f"\nEmail Case-Insensitivity: {passed}/{len(test_cases)} tests passed")
    return passed == len(test_cases)

def test_required_fields():
    """Test Fix #5: Required field validation"""
    print("\n" + "="*60)
    print("TEST FIX #5: Required Field Validation")
    print("="*60)
    
    email = "requiredtest@example.com"
    test_registration(email, "password123")
    
    base_data = {
        "email": email,
        "name": "Required Test",
        "idNumber": "STU222222",
        "department": "CSE",
        "degree": "B.Tech",
        "collegeName": "LMN College",
        "phone": "9222222222",
        "cgpa": 7.0,
        "branch": "CSE",
        "certifications": "",
        "backlogs": 0,
        "skills": {}
    }
    
    test_cases = [
        ({"name": ""}, False, "Missing: name"),
        ({"phone": ""}, False, "Missing: phone"),
        ({"department": ""}, False, "Missing: department"),
        ({"degree": ""}, False, "Missing: degree"),
        ({"collegeName": ""}, False, "Missing: collegeName"),
        ({"branch": ""}, False, "Missing: branch"),
        ({}, True, "All required fields present"),
    ]
    
    passed = 0
    for updates, should_pass, description in test_cases:
        data = base_data.copy()
        data.update(updates)
        
        resp = requests.post(f"{BASE_URL}/student/profile",
            json=data,
            headers=HEADERS, timeout=5)
        
        is_valid = (resp.status_code == 200)
        is_correct = (is_valid == should_pass)
        
        if is_correct:
            passed += 1
            symbol = "✓"
        else:
            symbol = "✗"
        
        status = "accepted" if is_valid else "rejected"
        expected = "should be accepted" if should_pass else "should be rejected"
        print(f"{symbol} {description}: {status} ({expected})")
    
    print(f"\nRequired Fields: {passed}/{len(test_cases)} tests passed")
    return passed == len(test_cases)

def main():
    print("\n" + "="*60)
    print("VALIDATION FIXES TEST SUITE")
    print("="*60 + "\n")
    
    if not health_check():
        print("\n❌ Cannot proceed: Server is not running")
        return
    
    results = {}
    
    try:
        results["Name Validation"] = test_name_validation()
        results["Phone Validation"] = test_phone_validation()
        results["CGPA Range Validation"] = test_cgpa_validation()
        results["Email Case-Insensitivity"] = test_email_case_insensitivity()
        results["Required Fields"] = test_required_fields()
    except Exception as e:
        print(f"\n❌ Error during testing: {e}")
        return
    
    # Summary
    print("\n" + "="*60)
    print("TEST SUMMARY")
    print("="*60)
    
    passed_count = sum(1 for v in results.values() if v)
    total_count = len(results)
    
    for test_name, result in results.items():
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{status}: {test_name}")
    
    print(f"\nTotal: {passed_count}/{total_count} validation tests passed")
    
    if passed_count == total_count:
        print("\n✅ ALL FIXES VERIFIED SUCCESSFULLY!")
    else:
        print(f"\n⚠️ {total_count - passed_count} test(s) still need attention")

if __name__ == "__main__":
    main()
