#!/usr/bin/env python3
import json
import sys
import os

def count_serialized_fields(json_file_path):
    """
    Read the serializedFields_alg1.json file and count the number of entries.
    
    The JSON structure is: Map<String, Map<String, String>>
    where the outer map keys are Java class names and values are inner maps
    containing field names and their types.
    """
    try:
        with open(json_file_path, 'r') as file:
            data = json.load(file)
        
        # Count the number of entries in the outer map
        count = len(data)
        
        print(f"File: {json_file_path}")
        print(f"Number of entries (Java classes): {count}")
        
        # Optional: Show some examples of the structure
        print(f"\nStructure example:")
        if count > 0:
            first_key = list(data.keys())[0]
            first_value = data[first_key]
            print(f"  Class: {first_key}")
            print(f"  Number of fields: {len(first_value)}")
            print(f"  Fields: {list(first_value.keys())[:3]}...")  # Show first 3 fields
        
        return count
        
    except FileNotFoundError:
        print(f"Error: File '{json_file_path}' not found.")
        return None
    except json.JSONDecodeError as e:
        print(f"Error: Invalid JSON format in '{json_file_path}': {e}")
        return None
    except Exception as e:
        print(f"Error reading file '{json_file_path}': {e}")
        return None

def main():
    # Default file path - you can change this or pass as command line argument
    default_file = "configInfo/hbase-2.1.9/serializedFields_alg1.json"
    
    if len(sys.argv) > 1:
        json_file = sys.argv[1]
    else:
        json_file = default_file
    
    count = count_serialized_fields(json_file)
    
    if count is not None:
        print(f"\nSummary: The JSON file contains {count} Java classes with serialized fields.")

if __name__ == "__main__":
    main() 