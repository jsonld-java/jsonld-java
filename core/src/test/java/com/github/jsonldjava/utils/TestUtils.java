/**
 *
 */
package com.github.jsonldjava.utils;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

/**
 * @author Peter Ansell p_ansell@yahoo.com
 *
 */
public class TestUtils {

    public static InputStream copyResourceToFileStream(File testDir, String resource)
            throws Exception {
        return new FileInputStream(copyResourceToFile(testDir, resource));
    }

    public static String copyResourceToFile(File testDir, String resource) throws Exception {
        String filename = resource;
        String directory = "";
        if (resource.contains("/")) {
            filename = resource.substring(resource.lastIndexOf('/'));
            directory = resource.substring(0, resource.lastIndexOf('/'));
        }
        final File nextDirectory = new File(testDir, directory);
        nextDirectory.mkdirs();
        final File nextFile = new File(nextDirectory, filename);
        nextFile.createNewFile();

        final InputStream inputStream = TestUtils.class.getResourceAsStream(resource);
        assertNotNull("Missing test resource: " + resource, inputStream);

        IOUtils.copy(inputStream, new FileOutputStream(nextFile));
        return nextFile.getAbsolutePath();
    }

    public static String join(Collection<String> list, String delim) {
        final StringBuilder builder = new StringBuilder();
        final Iterator<String> iter = list.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delim);
        }
        return builder.toString();
    }

    private TestUtils() {
    }

}
