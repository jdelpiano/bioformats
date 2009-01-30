//
// showinf.cpp
//

// A C++ version of the Bio-Formats showinf command line utility.

#include "jace/JNIHelper.h"

#include "jace/StaticVmLoader.h"
using jace::StaticVmLoader;

#include "jace/OptionList.h"
using jace::OptionList;

#include "jace/JArray.h"
using jace::JArray;

#include "jace/JNIException.h"
using jace::JNIException;

#include "jace/proxy/types/JBoolean.h"
using jace::proxy::types::JBoolean;

#include "jace/proxy/types/JByte.h"
using jace::proxy::types::JByte;

#include "bioformats.h"

#include <string>
using std::string;

#include <exception>
using std::exception;

#include <iostream>
using std::cout;
using std::endl;

int main(int argc, const char *argv[]) {
  try {
    // initialize the Java virtual machine
    cout << "Creating JVM..." << endl;
    StaticVmLoader loader(JNI_VERSION_1_4);
    OptionList list;
    list.push_back(jace::ClassPath(
      "jace-runtime.jar:bio-formats.jar:loci_tools.jar"));
    list.push_back(jace::CustomOption("-Xcheck:jni"));
    list.push_back(jace::CustomOption("-Xmx256m"));
    //list.push_back(jace::CustomOption("-verbose:jni"));
    jace::helper::createVm(loader, list, false);
    cout << "JVM created." << endl;

    // output command line arguments
    cout << "Arguments:" << endl;
    for (int i=0; i<argc; i++) cout << "\t#" << i << ": " << argv[i] << endl;

    /*
    typedef JArray<String> StringArray;
    StringArray args(argc - 1);
    for (int i=1; i<argc; i++) args[i - 1] = argv[i];
    JBoolean result = ImageInfo::testRead(args);
    */

    if (argc < 2) {
      cout << "Please specify a filename on the command line." << endl;
    }
    else {
      String id = argv[1];
      cout << "Initializing " << id << endl;
      ImageReader reader;
      reader.setId(id);
      int w = reader.getSizeX();
      int h = reader.getSizeY();
      cout << "Image planes are " << w << " x " << h << endl;
      reader.openBytes(0);
      //JBoolean result = ImageInfo::testRead(reader, args);

      //return result ? 0 : 1;
    }
  }
  catch (FormatException& fe) {
    cout << fe << endl;
    return -1;
  }
  catch (IOException& ioe) {
    cout << ioe << endl;
    return -2;
  }
  catch (JNIException& jniException) {
    cout << "An unexpected JNI error occurred. " << jniException.what() << endl;
    return -3;
  }
  catch (std::exception& e) {
    cout << "An unexpected C++ error occurred. " << e.what() << endl;
    return -4;
  }

}
