/**
 * Definition.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package semgen.webservices.KEGG;

public class Definition  implements java.io.Serializable {
<<<<<<< HEAD
	private static final long serialVersionUID = 1L;

	private String entry_id;

    private String definition;
=======
    private java.lang.String entry_id;

    private java.lang.String definition;
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d

    public Definition() {
    }

    public Definition(
<<<<<<< HEAD
           String entry_id,
           String definition) {
=======
           java.lang.String entry_id,
           java.lang.String definition) {
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
           this.entry_id = entry_id;
           this.definition = definition;
    }


    /**
     * Gets the entry_id value for this Definition.
     * 
     * @return entry_id
     */
<<<<<<< HEAD
    public String getEntry_id() {
=======
    public java.lang.String getEntry_id() {
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
        return entry_id;
    }


    /**
     * Sets the entry_id value for this Definition.
     * 
     * @param entry_id
     */
<<<<<<< HEAD
    public void setEntry_id(String entry_id) {
=======
    public void setEntry_id(java.lang.String entry_id) {
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
        this.entry_id = entry_id;
    }


    /**
     * Gets the definition value for this Definition.
     * 
     * @return definition
     */
<<<<<<< HEAD
    public String getDefinition() {
=======
    public java.lang.String getDefinition() {
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
        return definition;
    }


    /**
     * Sets the definition value for this Definition.
     * 
     * @param definition
     */
<<<<<<< HEAD
    public void setDefinition(String definition) {
=======
    public void setDefinition(java.lang.String definition) {
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
        this.definition = definition;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Definition)) return false;
        Definition other = (Definition) obj;
<<<<<<< HEAD
=======
        if (obj == null) return false;
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.entry_id==null && other.getEntry_id()==null) || 
             (this.entry_id!=null &&
              this.entry_id.equals(other.getEntry_id()))) &&
            ((this.definition==null && other.getDefinition()==null) || 
             (this.definition!=null &&
              this.definition.equals(other.getDefinition())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getEntry_id() != null) {
            _hashCode += getEntry_id().hashCode();
        }
        if (getDefinition() != null) {
            _hashCode += getDefinition().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Definition.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("SOAP/KEGG", "Definition"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("entry_id");
        elemField.setXmlName(new javax.xml.namespace.QName("", "entry_id"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("definition");
        elemField.setXmlName(new javax.xml.namespace.QName("", "definition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
<<<<<<< HEAD
           Class<?> _javaType,  
=======
           java.lang.String mechType, 
           java.lang.Class _javaType,  
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
<<<<<<< HEAD
           Class<?> _javaType,  
=======
           java.lang.String mechType, 
           java.lang.Class _javaType,  
>>>>>>> 2eb394907b98577f1b916408cf22a2de6952b22d
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
