package com.avast.syringe.config.perspective;

import freemarker.cache.TemplateLoader;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

/**
 * User: vacata
 * Date: 11/29/13
 * Time: 9:09 PM
 *
 * Delegating {@link TemplateLoader} adding support for usage of OS specific line separators ("\r\n" on Windows platform,
 * "\n" on Nix based platforms) for template rendering.
 */
public class OsSpecificTemplateLoader implements TemplateLoader {

    private TemplateLoader wrappedLoader;

    public OsSpecificTemplateLoader(TemplateLoader wrappedLoader) {
        this.wrappedLoader = wrappedLoader;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return wrappedLoader.findTemplateSource(name);
    }

    @Override
    public long getLastModified(Object templateSource) {
        return wrappedLoader.getLastModified(templateSource);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        Reader origReader = wrappedLoader.getReader(templateSource, encoding);
        List<String> lines = IOUtils.readLines(origReader);
        //The original reader is not necessary any longer
        origReader.close();

        StringWriter sw = new StringWriter();
        IOUtils.writeLines(lines, System.lineSeparator(), sw);
        StringReader sr = new StringReader(sw.getBuffer().toString());
        return sr;
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        wrappedLoader.closeTemplateSource(templateSource);
    }
}
