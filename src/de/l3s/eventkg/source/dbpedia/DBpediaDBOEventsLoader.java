package de.l3s.eventkg.source.dbpedia;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.pipeline.Config;
import de.l3s.eventkg.pipeline.Extractor;
import de.l3s.eventkg.util.FileLoader;
import de.l3s.eventkg.util.FileName;

public class DBpediaDBOEventsLoader extends Extractor {

	private PrintWriter resultsWriter;
	private PrintWriter resultsWriterBlacklist;

	public DBpediaDBOEventsLoader(List<Language> languages) {
		super("DBpediaDBOEventsLoader", Source.DBPEDIA,
				"Loads all DBpedia entities that are events, by looking for instances of e.g. <http://schema.org/Event>. It also maintains a set of blacklisted entities that may never be mapped to events, e.g. instances of <http://schema.org/Organization>.",
				languages);
	}

	public void run() {
		for (Language language : this.languages) {
			run(language);
		}
	}

	public void run(Language language) {

		Map<String, Set<String>> parentClasses = DBpediaTypesExtractor.parseOntology();

		Set<String> foundEvents = new HashSet<String>();
		Set<String> uniqueFoundEvents = new HashSet<String>();
		Set<String> targetObjects = loadEventObjects();
		Set<String> blacklistObjects = loadBlacklistClasses(language);

		try {
			resultsWriter = FileLoader.getWriter(FileName.DBPEDIA_DBO_EVENTS_FILE_NAME, language);
			resultsWriterBlacklist = FileLoader.getWriter(FileName.DBPEDIA_DBO_NO_EVENTS_FILE_NAME, language);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		BufferedReader br = null;

		if (FileLoader.fileExists(FileName.DBPEDIA_TYPES, language)) {

			try {
				try {
					br = FileLoader.getReader(FileName.DBPEDIA_TYPES, language);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}

				String line;
				while ((line = br.readLine()) != null) {
					if (line.startsWith("#"))
						continue;

					String[] parts = line.split(" ");
					String object = parts[2];
					object = object.substring(object.lastIndexOf("/") + 1, object.lastIndexOf(">"));

					boolean onBlackList = blacklistObjects.contains(object);
					if (!onBlackList && parentClasses.containsKey(object)) {
						onBlackList = !Collections.disjoint(parentClasses.get(object), blacklistObjects);
					}

					if (targetObjects.contains(object) || onBlackList || (parentClasses.containsKey(object)
							&& !Collections.disjoint(parentClasses.get(object), targetObjects))) {

						String subject = parts[0];
						String property = parts[1];

						if (!subject.contains("resource"))
							continue;

						subject = subject.substring(subject.lastIndexOf("resource/") + 9, subject.lastIndexOf(">"));

						String fileLine = subject + Config.TAB + property + Config.TAB + object;
						if (foundEvents.contains(fileLine))
							continue;

						// Manual corrections of erronous DBpedia entries
						if (subject.equals("Dunblane_school_massacre")
								|| subject.equals("San_Ysidro_McDonald's_massacre"))
							onBlackList = false;

						if (onBlackList)
							resultsWriterBlacklist.write(fileLine + Config.NL);
						else
							resultsWriter.write(fileLine + Config.NL);

						foundEvents.add(fileLine);
						uniqueFoundEvents.add(subject);
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					br.close();
					resultsWriter.close();
					resultsWriterBlacklist.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("Found " + uniqueFoundEvents.size() + " DBpedia (" + language + ") events.");
	}

	public static Set<String> loadEventObjects() {

		Set<String> targetProperties = new HashSet<String>();

		targetProperties.add("Event");
		targetProperties.add("SocietalEvent");

		return targetProperties;
	}

	public static Set<String> loadBlacklistClasses(Language language) {

		// create a set of entities that cannot be events.

		Set<String> blacklistObjects = new HashSet<String>();

		// each sub class or dbo:Organisation except for SportsLeague
		blacklistObjects.add("Broadcaster");
		blacklistObjects.add("Group");
		blacklistObjects.add("SportsClub");
		blacklistObjects.add("GovernmentAgency");
		blacklistObjects.add("Legislature");
		blacklistObjects.add("MilitaryUnit");
		blacklistObjects.add("PoliticalParty");
		blacklistObjects.add("SportsTeam");
		blacklistObjects.add("TradeUnion");
		blacklistObjects.add("EducationalInstitution");
		blacklistObjects.add("EmployersOrganisation");
		blacklistObjects.add("GeopoliticalOrganisation");
		blacklistObjects.add("InternationalOrganisation");
		blacklistObjects.add("Non-ProfitOrganisation");
		blacklistObjects.add("Parliament");
		blacklistObjects.add("ReligiousOrganisation");
		blacklistObjects.add("SambaSchool");
		blacklistObjects.add("TermOfOffice");

		// there is a bug in the Italian DBpedia which makes a lot of events
		// so:Person
		if (language != Language.IT)
			blacklistObjects.add("Person");

		return blacklistObjects;
	}
}
