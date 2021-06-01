package de.metahlfabric;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.json.JSONObject;

import javax.management.AttributeNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * The MetaDef defines the attributes and products available in this channel
 */

@DataType()
public class MetaDef {

    private enum ChangeType {ADD, DELETE}

    @Property
    ArrayList<AttributeDefinition> attributeDefinitions;

    @Property
    ArrayList<AssetDefinition> assetDefinitions;

    @Property
    ArrayList<String> units;

    /**
     * Class constructor
     */
    public MetaDef() {
        attributeDefinitions = new ArrayList<>();
        assetDefinitions = new ArrayList<>();
        units = new ArrayList<>();
    }

    /**
     * @return the list of defined units
     */
    public ArrayList<String> getUnitList() {
        return this.units;
    }

    /**
     * @param unit the unit definition to add
     */
    public void addUnitToUnitList(String unit) {
        this.units.add(unit);
    }

    /**
     * @return the hash map of defined attributes
     */
    public List<AttributeDefinition> getAttributeList() {
        return this.attributeDefinitions;
    }

    /**
     * @param attribute get data type for this attribute
     * @return the data typ for the specified attribute
     */
    public String getDataTypeByAttribute(String attribute) {
        for (AttributeDefinition attributeDefinition : this.attributeDefinitions) {
            if (attributeDefinition.name.equalsIgnoreCase(attribute))
                return attributeDefinition.dataType;
        }
        return "attribute not found";
    }

    /**
     * @param attribute the attribute to add
     * @param dataType  the data type of the attribute to add
     */
    public void addAttributeDefinition(String attribute, String dataType) {
        for (AttributeDefinition attributeDefinition : this.attributeDefinitions)
            if (attributeDefinition.getName().equalsIgnoreCase(attribute)) {
                attributeDefinition.setDataType(dataType);
                return;
            }
        this.attributeDefinitions.add(new AttributeDefinition(attribute, dataType));
    }

    /**
     * @param attribute the attribute to check
     * @return true if the attribute exits
     */
    public boolean attributeExists(String attribute) {
        for (AttributeDefinition attributeDefinition : this.attributeDefinitions)
            if (attributeDefinition.getName().equalsIgnoreCase(attribute))
                return true;
        return false;
    }

    /**
     * @return the HashMap of defined products
     */
    public List<AssetDefinition> getAssetDefinitions() {
        return this.assetDefinitions;
    }

    /**
     * @param assetName get attributes of this product
     * @return the attributes of the specified product
     */
    public List<AttributeDefinition> getAttributesByAssetName(String assetName) {
        for (AssetDefinition assetDefinition : this.assetDefinitions)
            if (assetDefinition.getName().equalsIgnoreCase(assetName))
                return assetDefinition.getAttributes();
        return null;
    }

    /**
     * @param assetName get attributes of this product
     * @return the attributes of the specified product
     */
    public List<AttributeDefinition> getAttributesByAssetNameAndVersion(String assetName, Integer version) {
        for (AssetDefinition assetDefinition : this.assetDefinitions)
            if (assetDefinition.getName().equalsIgnoreCase(assetName))
                return assetDefinition.getAttributes(version);
        return null;
    }

    /**
     * @param assetName  the product to add
     * @param attributes the attributes to add
     */
    public void addAssetDefinition(String assetName, List<AttributeDefinition> attributes) {
        for (AssetDefinition assetDefinition : this.assetDefinitions) {
            if (assetDefinition.getName().equalsIgnoreCase(assetName)) {
                // Asset already exists!
                return;
            }
        }

        this.assetDefinitions.add(new AssetDefinition(assetName, attributeDefinitions));
    }

    public boolean deleteAssetDefinition(String assetName) {
        return this.assetDefinitions.removeIf(assetDefinition -> assetDefinition.getName().equalsIgnoreCase(assetName));
    }

    /**
     * @param productName the product to check
     * @return true if the product exists
     */
    public boolean assetNameExists(String productName) {
        for (AssetDefinition assetDefinition : this.assetDefinitions)
            if (assetDefinition.getName().equalsIgnoreCase(productName))
                return true;
        return false;
    }

    /**
     * @return the object as a json string
     */
    public String toString() {
        return toJSONString();
    }

    /**
     * @return the object as a json string
     */
    public String toJSONString() {
        return new JSONObject(this).toString();
    }

    /**
     * @return the json object
     */
    public JSONObject toJSON() {
        return new JSONObject(this);
    }

    @DataType
    class AttributeDefinition {

        @Property
        private final String name;
        @Property
        private final ArrayList<String> dataTypeHistory;
        @Property
        private String dataType;
        @Property
        private Integer version;

        public AttributeDefinition(String name, String dataType) {
            this.dataTypeHistory = new ArrayList<>();
            this.name = name;
            this.dataType = dataType;
            this.version = 1;
        }

        public void setDataType(String dataType) {
            this.dataTypeHistory.add(this.dataType);
            this.dataType = dataType;
            this.version++;
        }

        public Integer getVersion() {
            return this.version;
        }

        public String getName() {
            return this.name;
        }

        public String getDataType() {
            return this.dataType;
        }

        public String getDataType(int version) {
            if (version == this.version)
                return dataType;
            else if (version > this.version || version < 1)
                return null;
            else
                return this.dataTypeHistory.get(version - 1);
        }

    }

    @DataType
    class AssetDefinition {
        @Property
        private final String name;
        @Property
        private final ArrayList<AttributeDefinition> attributes;
        @Property
        private Integer version;
        @Property
        private final ArrayList<AttributeChange> changeHistory;

        AssetDefinition(String name, ArrayList<AttributeDefinition> attributes) {
            this.name = name;
            this.attributes = attributes;
            this.version = 1;
            this.changeHistory = new ArrayList<>();
        }

        public String getName() {
            return this.name;
        }

        public List<AttributeDefinition> getAttributes() {
            return this.attributes;
        }

        public List<AttributeDefinition> getAttributes(int version) {
            if(version < 1)
                return null;
            ArrayList<AttributeDefinition> attributeDefinitionsOfVersion = new ArrayList<>();
            attributeDefinitionsOfVersion.addAll(this.attributes);

            for(int i = this.version - 1; i >= version; i--) {
                AttributeChange attributeChange = this.changeHistory.get(i);
                if(attributeChange.type.equals(ChangeType.ADD))
                    attributeDefinitionsOfVersion.remove(attributeChange.attribute);
                else
                    attributeDefinitionsOfVersion.add(attributeChange.attribute);
            }

            return attributeDefinitionsOfVersion;
        }

        void addAttribute(AttributeDefinition attribute) throws NullPointerException {
            if (attribute == null)
                throw new NullPointerException("Attempt to add an attribute but the parameter given was Null!");
            for (AttributeDefinition attributeDefinition : this.attributes)
                if (attributeDefinition.getName().equalsIgnoreCase(attribute.getName())
                        && attributeDefinition.getVersion().equals(attribute.getVersion()))
                    return;
            this.attributes.add(attribute);
            this.changeHistory.add(new AttributeChange(ChangeType.ADD, attribute));
            this.version++;
        }

        void removeAttribute(AttributeDefinition attribute) throws NullPointerException,
                AttributeNotFoundException {
            if (attribute == null)
                throw new NullPointerException("Attempt to remove an attribute but the parameter given was Null!");
            for (AttributeDefinition attributeDefinition : this.attributes)
                if (attributeDefinition.getName().equalsIgnoreCase(attribute.getName())
                        && attributeDefinition.getVersion().equals(attribute.getVersion())) {
                    this.attributes.remove(attributeDefinition);
                    this.changeHistory.add(new AttributeChange(ChangeType.DELETE, attribute));
                    this.version++;
                    return;
                }
            throw new AttributeNotFoundException("Attempt to remove an attribute which is not in the attribute list!");
        }

        boolean hasAttribute(String name) {
            for (AttributeDefinition attributeDefinition : this.attributes) {
                if (attributeDefinition.getName().equalsIgnoreCase(name))
                    return true;
            }
            return false;
        }

        Integer getVersion() {
            return this.version;
        }

        @DataType
        private class AttributeChange {
            @Property
            final ChangeType type;
            @Property
            final AttributeDefinition attribute;

            AttributeChange(ChangeType type, AttributeDefinition attribute) {
                this.type = type;
                this.attribute = attribute;
            }
        }

    }

}