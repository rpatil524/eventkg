package de.l3s.eventkg.source.dbpedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaEventRelationsExtractor extends Extractor {

	private PrintWriter eventResultsWriter;
	private PrintWriter eventLiteralResultsWriter;
	private PrintWriter entityResultsWriter;
	private AllEventPagesDataSet allEventPagesDataSet;

	public DBpediaEventRelationsExtractor(List<Language> languages, AllEventPagesDataSet allEventPagesDataSet) {
		super("DBpediaEventRelationsExtractor", Source.DBPEDIA,
				"Loads all DBpedia relations where the subject and/or object is an event or both have an existence time.",
				languages);
		this.allEventPagesDataSet = allEventPagesDataSet;
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {
		loadEventRelations(language);
		loadEventLiteralRelations(language);
	}

	public void loadEventRelations(Language language) {

		Set<String> forbiddenProperties = DBpediaEventLocationsExtractor.loadLocationProperties();
		forbiddenProperties.addAll(DBpediaPartOfLoader.loadPartOfProperties());
		forbiddenProperties.addAll(DBpediaPartOfLoader.loadNextEventProperties());
		forbiddenProperties.addAll(DBpediaPartOfLoader.loadPreviousEventProperties());

		BufferedReader br = null;

		try {
			eventResultsWriter = FileLoader.getWriter(FileName.DBPEDIA_EVENT_RELATIONS, language);
			entityResultsWriter = FileLoader.getWriter(FileName.DBPEDIA_ENTITY_RELATIONS, language);
			br = FileLoader.getReader(FileName.DBPEDIA_MAPPINGS, language);

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] parts = line.split(" ");
				String object = parts[2];
				String subject = parts[0];
				String property = parts[1];

				// TODO: ignore even more
				// ignore locations
				if (forbiddenProperties.contains(property)) {
					continue;
				}
				if (property.equals("rdf-schema#seeAlso") || property.equals("owl#differentFrom"))
					continue;

				if (!subject.contains("resource"))
					continue;

				if (!object.contains("resource"))
					continue;

				try {
					subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));
					object = object.substring(object.lastIndexOf("/") + 1, object.lastIndexOf(">"));
				} catch (StringIndexOutOfBoundsException e) {
					// skip objects like
					// "http://fr.dbpedia.org/resource/Sultanat_d'Égypte__1"@fr
					// .
					continue;
				}

				if (this.allEventPagesDataSet.getEventByWikipediaLabel(language, subject) != null
						|| this.allEventPagesDataSet.getEventByWikipediaLabel(language, object) != null) {
					eventResultsWriter.write(subject + Config.TAB + property + Config.TAB + object + Config.NL);
				} else if (this.allEventPagesDataSet.getEntityWithExistenceTimeByWikipediaLabel(language,
						subject) != null
						|| this.allEventPagesDataSet.getEntityWithExistenceTimeByWikipediaLabel(language,
								object) != null) {
					entityResultsWriter.write(subject + Config.TAB + property + Config.TAB + object + Config.NL);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				eventResultsWriter.close();
				entityResultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void loadEventLiteralRelations(Language language) {

		Set<String> forbiddenProperties = DBpediaEventLocationsExtractor.loadLocationProperties();
		forbiddenProperties.addAll(DBpediaTimesExtractor.loadTimeProperties().keySet());
		forbiddenProperties.addAll(loadLabelsAndDescriptionProperties());

		BufferedReader br = null;

		try {
			eventLiteralResultsWriter = FileLoader.getWriter(FileName.DBPEDIA_EVENT_LITERAL_RELATIONS, language);
			br = FileLoader.getReader(FileName.DBPEDIA_MAPPINGS_LITERALS, language);

			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#"))
					continue;

				String[] parts = line.split(" ");

				String object = "";
				for (int i = 2; i < parts.length; i++)
					object += parts[i] + " ";
				object = object.trim();

				String subject = parts[0];
				String property = parts[1];

				if (forbiddenProperties.contains(property)) {
					continue;
				}
				if (property.equals("rdf-schema#seeAlso") || property.equals("owl#differentFrom"))
					continue;

				if (!subject.contains("resource"))
					continue;

				try {
					subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));
					// object = object.substring(object.lastIndexOf("/") + 1,
					// object.lastIndexOf(">"));
				} catch (StringIndexOutOfBoundsException e) {
					// skip objects like
					// "http://fr.dbpedia.org/resource/Sultanat_d'Égypte__1"@fr
					// .
					continue;
				}

				if (this.allEventPagesDataSet.getEventByWikipediaLabel(language, subject) != null) {
					eventLiteralResultsWriter.write(subject + Config.TAB + property + Config.TAB + object + Config.NL);
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
				eventLiteralResultsWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private Set<String> loadLabelsAndDescriptionProperties() {
		Set<String> properties = new HashSet<String>();

		properties.add("<http://purl.org/dc/elements/1.1/description>");
		properties.add("<http://dbpedia.org/ontology/alias>");
		properties.add("<http://xmlns.com/foaf/0.1/name>");

		return properties;
	}

}
