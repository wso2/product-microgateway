import ballerina/math;
import ballerina/http;
import ballerina/log;
import ballerina/auth;
import ballerina/config;
import ballerina/runtime;
import ballerina/system;
import ballerina/time;
import ballerina/io;
import ballerina/reflect;
import ballerina/internal;

public type Result object {
    boolean valid;
    int errorCount;
    error[] resultErr;
    string[] getErrorMessages;
    string modelName;
};

function closeRc(io:ReadableCharacterChannel rc) {
    var result = rc.close();
    if (result is error){
        log:printError("Error occurred while closing character stream",
        err = result);
    }
}


function read(string path) returns json {
    io:ReadableByteChannel rbc = io:openReadableFile(path);
    io:ReadableCharacterChannel rch = new(rbc, "UTF8");
    var result = rch.readJson();
    if (result is error) {
        closeRc(rch);
        panic result;
    } else {
        closeRc(rch);
        return result;
    }
}

//validate all data types related attributes
public function valueValidator(string key, json value, json field) returns (error[]) {
    error[] errors;
    if (field[TYPE].toString() == STRING && field.format == null) {
        int min;
        int max;
        //check whether minLength property is defined for the string
        if (field.minLength != null) {
            min = <int>field.minLength;
        }
        //check whether maxLength property is defined for the string
        if (field.maxLength != null) {
            max = <int>field.maxLength;
        }
        //if the string have minLength/maxLength properties, validate them
        if (min > 0 || max > 0) {
            processError(errors, validateMinMaxLength(key, value, field.minLength, field.maxLength));
        }
    }
    //if a pattern is defined for the string, validate the pattern
    if (field.pattern != null) {
        processError(errors, validatePattern(key, value, field.pattern));
    }
    //if the type of the field is integer or number, validate values
    if (field[TYPE].toString() == INTEGER || field[TYPE].toString() == NUMBER) {
        if (typeOf(value) == INTEGER) {
            int intValue = <int>value;
            //if minimum/maximum properties are defined for the integer do the validation
            if (field.minimum != null || field.maximum != null) {
                processError(errors, validateMinMaxValue(key, intValue, field.minimum, field.maximum, false, false))
                ;
            }
            if (field.exclusiveMinimum != null || field.exclusiveMaximum != null) {
                boolean exclusiveMin;
                boolean exclusiveMax;

                if (field.exclusiveMinimum != null) {
                    exclusiveMin = <boolean>field.exclusiveMinimum;
                }
                if (field.exclusiveMaximum != null) {
                    exclusiveMax = <boolean>field.exclusiveMaximum;
                }
                //validate minimum/maximum with exclusiveMinimum/exclusiveMaximum properties
                if (field.minimum != null || field.maximum != null) {
                    processError(errors, validateMinMaxValue(key, intValue, field.minimum, field.maximum,
                            exclusiveMin, exclusiveMax));
                }
            }
        } else if (typeOf(value) == NUMBER) {
            float floatValue = <float>value;
            //if minimum/maximum properties are defined for the number do the validation
            if (field.minimum != null || field.maximum != null) {
                processError(errors, validateMinMaxValue(key, floatValue, field.minimum, field.maximum, false, false
                    ));
            }
            if (field.exclusiveMinimum != null || field.exclusiveMaximum != null) {
                boolean exclusiveMin = <boolean>field.exclusiveMinimum;
                boolean exclusiveMax = <boolean>field.exclusiveMaximum;
                //validate minimum/maximum with exclusiveMinimum/exclusiveMaximum properties
                if (field.minimum != null || field.maximum != null) {
                    processError(errors, validateMinMaxValue(key, floatValue, field.minimum, field.maximum,
                            exclusiveMin, exclusiveMax));
                }
            }
        }
    }
    //if field contains a enum validate it
    if (field.enum != null)  {
        processError(errors, validateEnums(key, value, field.enum));
    }
    return errors;//return the array of errors
}

public function validateMinMaxLength(string name, json value, json minLength, json maxLength) returns (boolean|error) {
    error err;
    int min = <int>minLength;
    int max = <int>maxLength;
    string newValue = value.toString();
    //if there is no minLength/maxLength property return null error object
    if (minLength == null && maxLength == null)  {
        return err;
    } else if (minLength == null) {//if there is a maxLength validate it
        return validateMaxLength(name, newValue, max);
    } else if (maxLength == null) {//if there is a minLength validate it
        return validateMinLength(name, newValue, min);
    }
    //if there is both minLength and maxLength peroperties, validate them
    if (newValue.length() < min || newValue.length() > max) {
        if (min <= 1){
         err = error(name + " cannot be blank and cannot be longer than " + maxLength.toString() +
                " characters long");
            return err;
        }
        err = error(name + " must be at least " + minLength.toString() + " characters long and no more than " +
            maxLength.toString() + " characters long");
        return err;
    }
    return true;
}
//validate maxLength of a string
public function validateMaxLength(string name, string value, int maxLength) returns (boolean|error) {
    error err;
    if (maxLength > 0){
        if (value.length() > maxLength){
        err = error(name + " must be no more than " + string.convert(maxLength) + " characters long");
        return err;
        }
    }
    return true;
}
//validate minLength of a string
public function validateMinLength(string name, string value, int minLength) returns (boolean|error) {
    error err;
    if (minLength > 0){
        if (value.length() < minLength){
            if (minLength == 1){
                err = error(name + " cannot be blank");
                return err;
            }
            err = error(name + " must be at least " + string.convert(minLength) + " characters long");
            return err;
        }
    }
    return true;
}
//validate whether given value is matching with the pattern defined in the schema
public function validatePattern(string name, json value, json pattern) returns (boolean|error) {
    //if there is no value or pattern no point of validating
    if (value == null || pattern == null) {
        return true;
    }
    string regex = pattern.toString();
    int length = regex.length();
    //substring the pattern in order to get rid of "^" and "$" symbols
    string newRegex = regex.substring(1, length);
    string stringValue = value.toString();
    //check whether the value is matching with the pattern and if not send an error
    if (check stringValue.matches(newRegex)) {
        return true;
    } else {
        error err = error(name + " does not match the pattern " + regex);
        return err;
    }
}

public function validateMinMaxValue(string name, int|float value, json minValue, json maxValue, boolean exclusiveMin
    , boolean exclusiveMax) returns (boolean|error) {
    int min = <int>minValue;
    int max = <int>maxValue;
    error err;
    if (minValue == null && maxValue == null) {//if there is no minimum/maximum no point of validating
        return true;
    } else if (maxValue == null && minValue != null) {//if there is only a minimum, validate it
        return validateMinValue(name, value, min, exclusiveMin);
    } else if (minValue == null && maxValue != null) {//if there is only a maximum, validate it
        return validateMaxValue(name, value, max, exclusiveMax);
    }
    //if there is both minimum and maximum properties do the validation
    if(value is int) {
        if (!exclusiveMin && !exclusiveMax && (value < min || value > max)) {
            err = error(name + " must be at least " + string.convert(min) + " and no more than " + string.convert(max));
            return err;
        } else if (exclusiveMin && exclusiveMax && (value <= min || value >= max)){
            err = error(name + " must be greater than " + string.convert(min) + " and less than " + string.convert(max));
            return err;
        } else if (exclusiveMin && (value <= min || value > max)){
            err = error(name + " must be greater than " + string.convert(min) + " and no more than " + string.convert(max));
            return err;
        } else if (exclusiveMax && (value >= max || value < min)){
            err = error(name + " must be at least " + string.convert(min) + " and less than " + string.convert(max));
            return err;
        }
    }
    else if (value is float) {
        if (!exclusiveMin && !exclusiveMax && (value < min || value > max)) {
            err = error(name + " must be at least " + string.convert(min) + " and no more than " + string.convert(max));
            return err;
        } else if (exclusiveMin && exclusiveMax && (value <= min || value >= max)){
            err = error(name + " must be greater than " + string.convert(min) + " and less than " + string.convert(max));
            return err;
        } else if (exclusiveMin && (value <= min || value > max)){
            err = error(name + " must be greater than " + string.convert(min) + " and no more than " + string.convert(max));
            return err;
        } else if (exclusiveMax && (value >= max || value < min)){
            err = error(name + " must be at least " + string.convert(min) + " and less than " + string.convert(max));
            return err;
        }
    }

    return true;
}
//validate the value with minimum and exclusiveMinimum properties
public function validateMinValue(string name, int|float value, int minValue, boolean exclusiveMin) returns (boolean|error) {
    error err;
    //value can be of type integer or float
    if(value is int) {
        if (!exclusiveMin && value < minValue) {//if there is no exclusiveMinimum property
            err = error(name + " must be at least " + string.convert(minValue));
            return err;
        } else if (exclusiveMin && value <= minValue) {//if there is exclusiveMinimum property
            err = error(name + " must be greater than " + string.convert(minValue));
            return err;
        }
    }

    else if(value is float) {
        if (!exclusiveMin && value < minValue) {//if there is no exclusiveMinimum property
            err = error(name + " must be at least " + string.convert(minValue));
            return err;
        } else if (exclusiveMin && value <= minValue) {//if there is exclusiveMinimum property
            err = error(name + " must be greater than " + string.convert(minValue));
            return err;
        }
    }
    return true;
}
//validate value with maximum and exclusiveMaximum property
public function validateMaxValue(string name, int|float value, int maxValue, boolean exclusiveMax) returns (boolean|error) {
    error err;
    //value can be of type integer or float
    if(value is int) {
        if (!exclusiveMax && value > maxValue) {//if there is no exclusiveMinimum property
            err = error(name + " must not be more than " + string.convert(maxValue));
            return err;
        } else if (exclusiveMax && value >= maxValue) {//if there is exclusiveMinimum property
            err = error(name + " must be less than " + string.convert(maxValue));
            return err;
        }
    }
    else if(value is float) {
        if (!exclusiveMax && value > maxValue) {//if there is no exclusiveMinimum property
            err = error(name + " must not be more than " + string.convert(maxValue));
            return err;
        } else if (exclusiveMax && value >= maxValue) {//if there is exclusiveMinimum property
            err = error(name + " must be less than " + string.convert(maxValue));
            return err;
        }
    }
    return true;
}
//validate enums
public function validateEnums(string name, json value, json enums) returns (boolean|error) {
    map<json> mapEnums = <map<json>> map<json>.convert(enums);
    //if there is no value doesn't need to validate
    if (value == null) {
        return true;
    }
    //iterate througth the enum map and check whether the value is matching with any item in the enum
    foreach var (k, v) in mapEnums {
        if (value == v) {
            return true;
        }
    }
    //if there is no match send an error
    error err = error(name + " is not set to an allowed value (see enum)");
    return err;
}
//put the incoming errors to the errors array
public function processError(error[] errors, boolean|error err) {
    if(err is error) {
        errors[errorItem] = err;
        errorItem = errorItem +1;
    }
}
//if there is allOf/discriminator property, create a new model by merging all referenced models
public function mergeModels(json target, json swaggerModel, json swaggerModels) returns (json) {
    json model = getMergedModel(swaggerModel, swaggerModels);
    model = getDiscriminatedModel(target, model, swaggerModels);
    return model;
}
//if there is allOf attribute, get the merged model
public function getMergedModel(json model, json models) returns (json) {
    map<json> mapModels = <map<json>> map<json>.convert(models);
    if (model.allOf != null) {
        json merged;
        foreach var (k, v) in mapModels {
            if (v[REFERENCE] != null) {
                var modelReference = models[replaceModelPrefix(v[REFERENCE].toString())];
                //if there is allOf property defined in the referenced model, again call the getMergedModel method
                if (modelReference.allOf != null) {
                    modelReference = getMergedModel(modelReference, models);
                }
                //merge the referenced model into the new model called merged
                merged = merge(merged, modelReference);

            } else {
                merged = merge(merged, v);
            }
        }
        return merged;
    }
    return model;
}
//if there are any references in the properties fields/discriminator property in the model merge them
public function dereferenceModel(json target, json swaggerModel, json swaggerModels) returns (json) {
    json model = getReferencedModel(swaggerModel, swaggerModels);
    model = getDiscriminatedModel(target, model, swaggerModels);
    return model;
}
//getting the model defined under the discriminator attribute
public function getDiscriminatedModel(json target, json model, json models) returns (json) {
    string subModelName = "";
    json newModel = model;
    if (target[model.discriminator.propertyName.toString()] != null) {
        subModelName = target[model.discriminator.propertyName.toString()].toString();
    }
    if (subModelName.length() > 0 && models[subModelName] != null) {
        var discriminator = model.discriminator;
        //discriminated model will be assigned to the newModel
        newModel = models[subModelName];
        //if there is allOf property in the discriminated model, get the merged model
        newModel = getMergedModel(newModel, models);
        newModel[DISCRIMINATOR] = discriminator;//discriminator property will be added to the newModel
    }
    return newModel;
}
//if there are any references in the properties fields, get those referenced models and merge them to the properties
//fields
public function getReferencedModel(json model, json models) returns (json) {
    json outModel = model;
    if (model.properties != null) {
        var keys = model.properties.getKeys();
        foreach var key in keys {
            var item = outModel.properties[key];
            //if there is a reference get the referenced model
            if (item != null && item[REFERENCE] != null) {
                string reference = item[REFERENCE].toString();
                var modelReference = models[replaceModelPrefix(reference)];

                if (modelReference != null) {
                    //if there is allOf property in the referenced model get the merged model
                    if (modelReference.allOf != null) {
                        modelReference = getMergedModel(modelReference, models);
                    }
                    //if there are references inside the referenced model, get them too
                    modelReference = getReferencedModel(modelReference, models);
                    //merge refrenced models in to the properties fields
                    outModel.properties[key] = merge(item, modelReference);
                }
            }
        }
    }
    return outModel;
}
//merge details of a source model to another given model
public function merge(json target, json source) returns (json) {
    foreach var key in source.getKeys() {
        var sourceProperty = source[key];
        if (target[key] == null) {//if there are no such property in the target add it
            target[key] = sourceProperty;
        } else {
            //if there is a value available for this property in the source model, check whether there are any
            //exceptional keys inside that property
            if (sourceProperty.getKeys().length() > 0) {
                foreach var proprtyKey in sourceProperty.getKeys(){
                    if (target[key][proprtyKey] == null) {
                        target[key][proprtyKey] = sourceProperty[proprtyKey];

                    }
                }
            }
        }
    }
    return target;
}

//creating a return object
public function createReturnObject(error[]|error err, string modelName) returns (Result) {
    Result result = new();
    if(err is error) {
        if (err.reason() == "") {
            result.valid = true;
            result.errorCount = 0;
        } else {
            result.valid = false;
            result.errorCount = 1;
            result.resultErr[0] = err;
        }
    } else if(err is error[]) {
        if (err.length() == 0) {
            result.valid = true;
            result.errorCount = 0;
        } else {
            result.valid = false;
            result.errorCount = err.length();
            result.resultErr = err;

            int index = 0;
            foreach var item in err {
                result.getErrorMessages[index] = item.reason();
                index = index +1;
            }
        }
    }
    if (modelName.length() > 0) {
        result.modelName = modelName;
    }
    return result;
}
//validating values
public function validateValue(string key, json field, json value, json models) returns (Result) {
    Result result = new();
    if (value != null) {
        //validate the type
        var typeErrors = validateType(key, value, field, models);
        if (typeErrors != null) {
            if (!typeErrors.valid && typeErrors.errorCount > 0) {
                return createReturnObject(typeErrors.resultErr, " ");
            }
        }
        error[] valueErrors = valueValidator(key, value, field);
        if (valueErrors.length() > 0) {
            return createReturnObject(valueErrors, " ");
        }
    }
    result.valid = true;
    return result;
}
//validating values with types defined in the fields
public function validateType(string name, json value, json field, json models) returns (Result) {
    Result result = new();
    //getting type defined in the model/field
    string expectedType = field[TYPE].toString();
    if (expectedType.length() > 0) {//if the field doesn't have a type it may have a reference to another model
        if (models != null && field[REFERENCE] != null) {
            //getting the reference to another model and replace it to the referenced model name
            string fieldReference = replaceModelPrefix(field[REFERENCE].toString());
            //validate the target object with the referenced model
            return validate(name, value, models[fieldReference], models);
        } else {
            return result;
        }

    } else {
        expectedType = expectedType.toLower();
    }
    //if expectedType is a object, validate the object
    if (expectedType == OBJECT) {
        return validateObject(name, value, field, models);
    }
    //if expectedType is an array, validate the array
    if (expectedType == ARRAY) {
        return validateArray(name, value, field, models);
    }
    //get the format of the field
    string format = field.format.toString();

    if (format != NULL) {
        format = format.toLower();
    }

    if (value == null) {//if there is no value, no point of validating
        return result;
    } else if (validateExpectedType(expectedType, value, format)) {//validate value and format
        result.valid = true;
        //if the field is of expected type then that value is valid
        return result;
    } else {
        error err = error(value.toString() + " is not the type, " + expectedType);
        return createReturnObject(err, " ");
    }
}
//validating an object
public function validateObject(string name, json value, json field, json models) returns (Result) {
    Result result = new();
    //if field contains properties, validate the object
    if (field != null && field.properties != null) {
        return validate(name, value, field, models);
    } else {
        result.valid = true;
        return result;
    }
}
//validating an array
public function validateArray(string name, json value, json field, json models) returns (Result) {
    map<json> mapValue = <map<json>> map<json>.convert(value);
    Result result = new();
    if (typeOf(value) != ARRAY) {//if the provided value is not an array send an error
        error err = error(value.toString() + " is not an array. An array is expected.");
        return createReturnObject(err, " ");
    }
    int minItems = 0;
    int maxItems = 0;
    var arrayType = "";
    var countItems = 0;
    int j = 0;
    if (field[ITEMS] != null && field[ITEMS][TYPE] != null) {
        // These items are a baser type and not a referenced model(ex:an integer array)
        if (field.minItems != null) {
            minItems = <int>field.minItems;
        }
        if (field.maxItems != null) {
            maxItems = <int>field.maxItems;
        }
        var fieldType = field[ITEMS][TYPE];
        error[] firstErrorArray = [];
        //loop each item in the array and validate them
        foreach var (k,v) in mapValue {
            json newVal = v;
            var valueErrors = validateValue(name + string.convert(countItems), field[ITEMS], newVal, models);
            if (valueErrors != null && !valueErrors.valid && valueErrors.errorCount > 0) {
                //if there are any errors add them to the firstErrorArray
                foreach var y in valueErrors.resultErr {
                    firstErrorArray[j] = y;
                    j = j + 1;
                }
                countItems = countItems + 1;//count the number of items in the array
            }
        }
        if (firstErrorArray.length() > 0) {
            return createReturnObject(firstErrorArray, "Array of " + fieldType.toString() + " (" + name + ")");
        }
        arrayType = fieldType.toString();
    } else if (field[ITEMS] != null && field[ITEMS][REFERENCE] != null) {
        // These items are a referenced model
        var fieldReference = replaceModelPrefix(field[ITEMS][REFERENCE].toString());
        //getting the name of the referenced model
        var model = models[fieldReference];
        //getting the referenced model
        if (model != null) {
            error[] secondErrorArray;
            int k = 0;
            //loop each item in the array and validate them against the referenced model
            foreach var (q, r) in mapValue {
                json newVal = r;
                var validationErrors = validate(name, newVal, model, models);
                if (validationErrors != null && !validationErrors.valid && validationErrors.errorCount > 0) {
                    //if there are any errors add them to the secondErrorArray
                    foreach var x in validationErrors.resultErr {
                        secondErrorArray[k] = x;
                        k = k + 1;
                    }
                }
                countItems = countItems + 1;//count the number of items in the array
            }
            if (secondErrorArray.length() > 0) {
                return createReturnObject(secondErrorArray, "Array of " + field[ITEMS][REFERENCE].toString() + " ("
                        + name + ")");
            }
        }
        arrayType = field[ITEMS][REFERENCE].toString();
    }
    error[] thirdErrorArray;
    int i = 0;
    //if the items in the array is less than minItems, send an error
    if (minItems > 0 && countItems < minItems) {
        error err = error("Array requires at least " + minItems + " item(s) and has " + countItems +
            " item(s).");
        thirdErrorArray[i] = err;
        i = i + 1;
        return createReturnObject(thirdErrorArray, "Array of " + arrayType + " (" + name + ")");
    }
    //if the items in the array is greater than maxItems send an error
    if (maxItems > 0 && countItems > maxItems) {
        error err = error("Array requires no more than " + maxItems + " item(s) and has " + countItems +
            " item(s).");
        thirdErrorArray[i] = err;
        i = i + 1;
        return createReturnObject(thirdErrorArray, "Array of " + arrayType + " (" + name + ")");
    }
    result.valid = true;
    return result;
}
//validating whether the provided value is of the expected type
public function validateExpectedType(string expectedType, json value, string format) returns (boolean) {
    if (expectedType == STRING){
        if (isStringType(value, format)) {
            return true;
        }
    } else if (expectedType == BOOLEAN) {
        if (isExpectedType(value, expectedType)) {
            return true;
        }
    } else if (expectedType == INTEGER) {
        if (isIntegerType(value, format)) {
            return true;
        }
    } else if (expectedType == NUMBER) {
        if (isNumberType(value, format)) {
            return true;
        }
    }
    return false;
}
//validating whether the provided value is of string type
public function isStringType(json value, string format) returns (boolean) {
    string stringValue = value.toString();
    if (isExpectedType(value, STRING)) {
        if (format.length() > 0) {
            return true;
        } else if (format == DATE || format == DATE_TIME) {
            boolean state = true;
            time:Time | error time1 = trap time:parse(stringValue, YYYY_MM_DD);
                if (time1 is time:Time) {
                  return state;
                } else if (time1 is error) {
                  state = false;
                }
                //parse the string value to check whether it is in the correct format
        } else {
            return true;
        }
    }
    return false;
}

//validating whether the provided value is of integer type
public function isIntegerType(json value, string format) returns (boolean) {
    if (!isExpectedType(value, INTEGER)) {
        return false;
    }
    if (format.length() > 0) {
        return true;
    } else if (format == INT_32) {//if the value is of int32 format, validate it
        var int32Max = math:pow(2.0, 31.0) - 1;
        int int32Value =  <int>value;
        //check whether the integer value is in the int32 value range
        if (int32Value >= - (int32Max + 1) && int32Value <= int32Max) {
            return true;
        }
        return false;
    } else if (format == INT_64) {//if the value is of int64 format, validate it
        var int64Max = math:pow(2.0, 63.0) - 1;
        int int64Value =  <int>value;
        //check whether the integer value is in the int64 value range
        if (int64Value >= - (int64Max + 1) && int64Value <= int64Max) {
            return true;
        }
        return false;
    } else {
        return true;
    }
}
//validating whether the provided value is of number type
public function isNumberType(json value, string format) returns (boolean) {
    if (typeOf(value) == INTEGER) {
        return isIntegerType(value, format);
    } else if (typeOf(value) == NUMBER) {
        return true;
    } else {
        return false;
    }
}
//return whether the type of the value is same as the expected type
public function isExpectedType(json value, string expectedType) returns (boolean) {
    string typeof;

    if(value is int) {typeof = INTEGER;}
    else if (value is float) {typeof = NUMBER;}
    else if(value is string) {typeof = STRING;}
    else if (value is boolean) {typeof = BOOLEAN;}
    else if(value is json) {typeof = JSON;}
    return expectedType == typeof;
}

public function validateSpecification(string name, json target, json model, json models) returns (error[]) {
    map<json> mapValue = <map<json>> map<json>.convert(target);
    //getting the properties defined in the model
    var properties = model.properties;
    error[] errorArray = [];
    int j = 0;
    //if there are no properties, it's a reference to a primitive type
    if (properties == null) {
        //if there is no type defined, no point of validating the value
        if (model[TYPE] == null) {
            return errorArray;
        }
        //validate the primitive type
        var singleValueErrors = validateValue(name, model, target, models);
        if (singleValueErrors.valid == false && singleValueErrors.errorCount != 0) {
            foreach var i in singleValueErrors.resultErr {
                errorArray[j] = i;
                j = j + 1;
            }
        }
    } else {//if there are properties, it means it is a model
        if (typeOf(target) != ARRAY) {
            errorArray = validateProperties(target, model, models);
            if (model.discriminator != null && errorArray.length() >= 1) {
                //if the parent model have the discriminator property but the discriminated model doesn't contain
                //that property
                if (errorArray.length() == 1 && errorArray[0].reason() == "Target property " + model.discriminator.
                propertyName.toString() + " is not in the model") {
                    // remove discriminator if it is the only error.
                    errorArray = [];
                } else {
                    error[] tempErrors = errorArray;
                    errorArray = [];
                    foreach var item in tempErrors{
                        if (item.reason() != "Target property " + model.discriminator.propertyName.toString() +
                            " is not in the model") {
                            errorArray[j] = item;
                            j = j + 1;
                        }
                    }
                }
            }
            //get values given in the target object for each property defined in the model and validate them
            foreach var key in properties.getKeys() {
                var field = properties[key];
                //getting the values for the properties defined in the model
                var value = target[key];
                //getting the values provided for each property in the model
                if (value != null) {
                    var valueErrors = validateValue(key, field, value, models);
                    if (!valueErrors.valid && valueErrors.errorCount > 0) {
                        foreach var t in valueErrors.resultErr {
                            errorArray[j] = t;
                            j = j + 1;
                        }
                    }
                }
            }
        } else {
            foreach var(k, v) in mapValue {
                errorArray = validateProperties(v, model, models);
                foreach var key in properties.getKeys() {
                    var field = properties[key];
                    //getting the values for the properties defined in the model
                    var value = v[key];
                    //getting the values provided for each property in the model
                    if (value != null) {
                        var valueErrors = validateValue(key, field, value, models);
                        if (!valueErrors.valid && valueErrors.errorCount > 0) {
                            foreach var t in valueErrors.resultErr {
                                errorArray[j] = t;
                                j = j + 1;
                            }
                        }
                    }
                }
            }
        }
    }
    return errorArray;
}
//return the type of the target object
public function typeOf(json target) returns (string) {
    string typeof;
    if(target is int) {typeof = INTEGER;}
    else if (target is float) {typeof = NUMBER;}
    else if(target is string) {typeof = STRING;}
    else if (target is boolean) {typeof = BOOLEAN;}
    else if(target is json) {typeof = OBJECT;}
    else if(target is json[]) {typeof = ARRAY;}
    return typeof;
}

public function validate(string name, json target, json swaggerModel, json swaggerModels) returns (Result) {
    error err;
    if (target == null)  {
        err = error("Unable to validate an undefined value of property: ");
        return createReturnObject(err, " ");
    }
    if (swaggerModel == null) {
        err = error("Unable to validate against an undefined model.");
        return createReturnObject(err, " ");
    }
    var targetType = typeOf(target);
    string modelType;
    if (swaggerModel[TYPE] != null) {
        modelType = swaggerModel[TYPE].toString();
    } else if (isType) {
        modelType = pathType;
    } else {
        modelType = OBJECT;
    }
    //compare the type of the target and model
    if (targetType != modelType) {
        err = error("Unable to validate a model with a type: " + targetType + ", expected: " + modelType);
        return createReturnObject(err, " ");
    }
    //if there is allOf/discriminator properties, create a new model by merging all referenced models
    json model = mergeModels(target, swaggerModel, swaggerModels);
    //validating required fields
    if (model.required != null && model.required.length() > 0) {
        error[] requireFieldErrors;
        requireFieldErrors = validateRequiredFields(target, model.required, model.properties);
        if (requireFieldErrors.length() > 0) {
            return createReturnObject(requireFieldErrors, " ");
        }
    }
    var validationErrors = validateSpecification(name, target, model, swaggerModels);
    if (validationErrors.length() > 0 && validationErrors.length() > 0) {
        return createReturnObject(validationErrors, model.id.toString());
    }
    return createReturnObject(err, " ");
}
//validate required fields
public function validateRequiredFields(json target, json fields, json modelFields) returns (error[]) {
    map<json> mapValue = <map<json>> map<json>.convert(target);
    int j = 0;
    error[] errorArray =[];
    //required field should be an array, if not send an error
    if (typeOf(fields) != ARRAY) {
        error err = error("fields must be an array of required fields");
        errorArray[j] = err;
        j = j + 1;
        return errorArray;
    }
    int i = 0;
    //go through each item in the required array and check whether the target object includes all of those required
    //fields
    while (i < fields.length()) {
        var property = fields[i].toString();
        //if the target object is an array check whether each item includes required fields
        if (typeOf(target) == ARRAY) {
            foreach var(k, v) in mapValue {
              if (v[property] == null || modelFields[property] == null) {
                error err = error(property + " is a required field");
                errorArray[j] = err;
                j = j + 1;
              }
            }
         } else if (target[property] == null || modelFields[property] == null) {
            error err = error(property + " is a required field");
            errorArray[j] = err;
            j = j + 1;
        }
        i = i + 1;
    }
    return errorArray;
}

//validating propertries in the JSON object against the properties of the models defined in the schema.
public function validateProperties(json target, json model, json models) returns (error[]) {
    var targetKeys = target.getKeys();
    error[] errorArray;
    int i = 0;
    //get all details in referenced models and merge them into a one model
    var referenceModel = dereferenceModel(target, model, models);
    //if the properties provided in the target object is not defined in the model, send an error
    if (targetKeys.length() > 0) {
        foreach var key in targetKeys{
            if (referenceModel.properties[key] == null) {
                error err = error("Target property " + key + " is not in the model");
                errorArray[i] = err;
                i = i + 1;
            }
        }
    }
    return errorArray;
}
//replace model references to the model name
public function replaceModelPrefix(string name) returns (string) {
    string newName;
    if (name.contains(DEFINITIONS)) {
        newName = name.replace(DEFINITIONS, "");
    }
    if (name.contains(COMPONENTS_SCHEMAS)) {
        newName = name.replace(COMPONENTS_SCHEMAS, "");
    }
    return newName;
}