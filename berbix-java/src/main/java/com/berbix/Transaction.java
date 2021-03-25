package com.berbix;

import java.time.ZonedDateTime;
import java.util.List;

public class Transaction {
  public String action;
  public ZonedDateTime completedAt;
  public ZonedDateTime createdAt;
  public String customerUid;
  public String dashboardUrl;
  List<Duplicate> duplicates;
  public String entity;
  public Fields fields;
  public List<String> flags;
  public Long id;
  public String implementationInfo;

  public static class Duplicate {
    public String customerUid;
    public Long transactionId;
  }

  public static class Fields {
    public FieldEntry addressCity;
    public FieldEntry addressCountry;
    public FieldEntry addressPostalCode;
    public FieldEntry addressStreet;
    public FieldEntry addressSubdivision;
    public FieldEntry addressUnit;
    public FieldEntry age;
    public FieldEntry dateOfBirth;
    public FieldEntry emailAddress;
    public FieldEntry familyName;
    public FieldEntry givenName;
    public FieldEntry idExpiryDate;
    public FieldEntry idIssueDate;
    public FieldEntry idIssuer;
    public FieldEntry idNumber;
    public FieldEntry idType;
    public FieldEntry middleName;
    public FieldEntry nationality;
    public FieldEntry phoneNumber;
    public FieldEntry sex;

    public static class FieldEntry {
      public String confidence;
      public List<Source> sources;
      public String value;

      public static class Source {
        public String confidence;
        public String type;
        public String value;
      }
    }
  }
}
