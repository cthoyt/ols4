
package uk.ac.ebi.ols.apitester;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class App 
{
    public static void main( String[] args ) throws MalformedURLException, IOException
    {
	Options options = new Options();

        Option optUrl = new Option(null, "url", true, "URL of a running OLS4 instance");
        optUrl.setRequired(true);
        options.addOption(optUrl);

        Option optOutDir = new Option(null, "outDir", true, "Directory to write output to");
        optOutDir.setRequired(true);
        options.addOption(optOutDir);

        Option optCompareDir = new Option(null, "compareDir", true, "Directory to compare output with");
        optCompareDir.setRequired(true);
        options.addOption(optCompareDir);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        String url = cmd.getOptionValue("url");
        String outDir = cmd.getOptionValue("outDir");
	String compareDir = cmd.getOptionValue("compareDir");

	System.exit(
		(new Ols4ApiTester(url, outDir, compareDir).test() ? 0 : 1)
	);
    }
}
