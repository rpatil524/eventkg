package de.l3s.eventkg.source.wikidata.processors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentDumpProcessor;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

/**
 * Extracts the following pairs for each item: <br>
 * file1: <item-id> <item-wiki-page-lang> file2: <item-id>
 * <item-wiki-label-lang> file3: <item-id> <item-wiki-description-lang> file4:
 * <item-id> <item-wiki-alias1-lang> <item-wiki-alias2-lang> ...
 */
public class PropertyNamesProcessor implements EntityDocumentDumpProcessor {

	private int itemCount = 0;
	private int itemCountProperties = 0;

	private List<Language> languages;

	private Map<Language, PrintStream> outLabelsProperties;

	private Map<Language, PrintStream> outDescriptionsProperties;

	private Map<Language, PrintStream> outAliasesProperties;

	public PropertyNamesProcessor(List<Language> languages) throws IOException {
		// open files for writing results
		this.languages = languages;

		this.outLabelsProperties = new HashMap<Language, PrintStream>();
		this.outDescriptionsProperties = new HashMap<Language, PrintStream>();
		this.outAliasesProperties = new HashMap<Language, PrintStream>();

		for (Language language : this.languages) {
			this.outLabelsProperties.put(language,
					FileLoader.getPrintStream(FileName.WIKIDATA_LABELS_PROPERTIES, language));
			this.outDescriptionsProperties.put(language,
					FileLoader.getPrintStream(FileName.WIKIDATA_DESCRIPTIONS_PROPERTIES, language));
			this.outAliasesProperties.put(language,
					FileLoader.getPrintStream(FileName.WIKIDATA_ALIASES_PROPERTIES, language));
		}
	}

	@Override
	public void processItemDocument(ItemDocument itemDocument) {
		this.itemCount++;

		// Print progress every 100,000 items:
		if (this.itemCount % 100000 == 0) {
			printStatus();
		}
	}

	@Override
	public void processPropertyDocument(PropertyDocument propertyDocument) {

		this.itemCountProperties++;

		for (Language language : this.languages) {

			MonolingualTextValue labelValue = propertyDocument.getLabels().get(language.getLanguageLowerCase());

			if (labelValue != null) {
				outLabelsProperties.get(language).println(
						propertyDocument.getEntityId().getId() + Config.TAB + csvEscape(labelValue.getText()));
			}

			List<String> aliases = new ArrayList<String>();
			List<MonolingualTextValue> aliasValues = propertyDocument.getAliases().get(language.getLanguageLowerCase());
			if (aliasValues != null) {
				for (MonolingualTextValue val : aliasValues) {
					aliases.add(csvEscape(val.getText()));
				}
			}
			if (!aliases.isEmpty())
				outAliasesProperties.get(language).println(propertyDocument.getEntityId().getId() + Config.TAB
						+ StringUtils.join(aliases, Config.TAB) + "");

			String description = null;
			MonolingualTextValue descriptionValue = propertyDocument.getDescriptions()
					.get(language.getLanguageLowerCase());
			if (descriptionValue != null) {
				description = descriptionValue.getText();
			}
			if (description != null)
				outDescriptionsProperties.get(language)
						.println(propertyDocument.getEntityId().getId() + Config.TAB + csvEscape(description));
		}
	}

	private String csvEscape(String string) {
		if (string == null)
			return "\\N";
		else
			return string.replaceAll("\t", "   ");
	}

	public void printStatus() {
		System.out
				.println("Found " + this.itemCount + " entity items and " + this.itemCountProperties + " properties.");
	}

	public void close() {
		printStatus();
		for (Language language : this.languages) {
			this.outLabelsProperties.get(language).close();
			this.outDescriptionsProperties.get(language).close();
			this.outAliasesProperties.get(language).close();
		}
	}

	public static FileOutputStream openExampleFileOutputStream(String filename) throws IOException {
		return new FileOutputStream(filename);
	}

	@Override
	public void open() {
	}

}
