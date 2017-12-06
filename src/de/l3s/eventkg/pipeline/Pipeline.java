package de.l3s.eventkg.pipeline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.l3s.eventkg.integration.AllEventPagesDataSet;
import de.l3s.eventkg.integration.DataCollector;
import de.l3s.eventkg.integration.DataSets;
import de.l3s.eventkg.integration.DataStoreWriter;
import de.l3s.eventkg.integration.LocationsIntegrator;
import de.l3s.eventkg.integration.SubLocationsCollector;
import de.l3s.eventkg.integration.TemporalRelationsCollector;
import de.l3s.eventkg.integration.TimesIntegrator;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;
import de.l3s.eventkg.source.currentevents.CurrentEventsRelationsExtraction;
import de.l3s.eventkg.source.currentevents.EventsFromFileExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaAllLocationsLoader;
import de.l3s.eventkg.source.dbpedia.DBpediaDBOEventsLoader;
import de.l3s.eventkg.source.dbpedia.DBpediaEventLocationsExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaEventRelationsExtractor;
import de.l3s.eventkg.source.dbpedia.DBpediaPartOfLoader;
import de.l3s.eventkg.source.dbpedia.DBpediaTimesExtractor;
import de.l3s.eventkg.source.wikidata.EventsFromFileFinder;
import de.l3s.eventkg.source.wikidata.WikidataExtractionWithEventPages;
import de.l3s.eventkg.source.wikidata.WikidataExtractionWithoutEventPages;
import de.l3s.eventkg.source.wikipedia.LabelsAndDescriptionsExtractor;
import de.l3s.eventkg.source.wikipedia.WikipediaEventsByCategoryNameLoader;
import de.l3s.eventkg.source.wikipedia.WikipediaLinkCountsExtractor;
import de.l3s.eventkg.source.wikipedia.WikipediaLinkSetsExtractor;
import de.l3s.eventkg.source.yago.YAGOEventLocationsExtractor;
import de.l3s.eventkg.source.yago.YAGOEventRelationsExtractor;
import de.l3s.eventkg.source.yago.YAGOExistenceTimeExtractor;
import de.l3s.eventkg.textual_events.TextualEventsExtractor;

public class Pipeline {

	private List<Language> languages;
	private AllEventPagesDataSet allEventPagesDataSet;

	public static void main(String[] args) {

		Config.init(args[0]);
		List<Language> languages = new ArrayList<Language>();
		for (String language : Config.getValue("languages").split(",")) {
			languages.add(Language.getLanguage(language));
		}

		Set<Integer> steps = new HashSet<Integer>();
		if (args.length > 0) {
			String arg1 = args[1];
			for (String stepString : arg1.split(",")) {
				steps.add(Integer.valueOf(stepString));
			}
		}

		Pipeline pipeline = new Pipeline(languages);
		pipeline.initDataSets();

		if (steps.contains(1)) {
			System.out.println("Step 1: Download files.");
			pipeline.download();
		} else
			System.out.println("Skip step 1: Download files.");

		if (steps.contains(2)) {
			System.out.println("Step 2: Start extraction -> Find event pages and extract relations.");
			pipeline.pipelineStep2();
		} else
			System.out.println("Skip step 2: Start extraction -> Find event pages and extract relations.");

		if (steps.contains(3)) {
			System.out.println("Step 3: Integration step 1.");
			pipeline.pipelineStep3();
		} else
			System.out.println("Skip step 3: Integration step 1.");

		if (steps.contains(4)) {
			System.out.println(
					"Step 4: Continue extraction -> Extract relations between events and entities with existence times.");
			pipeline.pipelineStep4();
		} else
			System.out.println("Skip step 4: Continue extraction -> Extract relations between events.");

		if (steps.contains(5)) {
			System.out.println("Step 5: Integration step 2.");
			pipeline.pipelineStep5();
		} else
			System.out.println("Skip step 5: Integration step 2.");
	}

	public Pipeline(List<Language> languages) {
		this.languages = languages;
	}

	private void download() {
		RawDataDownLoader downloader = new RawDataDownLoader(languages);
		// downloader.createFolders();
		// downloader.copyMetaFiles();
		downloader.downloadFiles();
	}

	private void pipelineStep2() {

		List<Extractor> extractors = new ArrayList<Extractor>();

		// Extraction from raw data

		// WCE
		extractors.add(new EventsFromFileExtractor(languages));

		// YAGO
		extractors.add(new YAGOExistenceTimeExtractor(languages));
		extractors.add(new YAGOEventLocationsExtractor(languages));

		// dbPedia
		extractors.add(new DBpediaDBOEventsLoader(languages));
		extractors.add(new DBpediaEventLocationsExtractor(languages));
		extractors.add(new DBpediaTimesExtractor(languages));
		extractors.add(new DBpediaPartOfLoader(languages));

		// Wikipedia
		extractors.add(new WikipediaEventsByCategoryNameLoader(languages));

		// Wikidata
		extractors.add(new WikidataExtractionWithoutEventPages(languages));

		for (Extractor extractor : extractors) {
			System.out.println(extractor.getName() + ", " + extractor.getSource() + " - " + extractor.getDescription());
			extractor.run();
		}
	}

	private void pipelineStep3() {

		List<Extractor> extractors = new ArrayList<Extractor>();

		extractors.add(new EventsFromFileFinder(languages));
		extractors.add(new DBpediaAllLocationsLoader(languages, getAllEventPagesDataSet()));
		// First step of integration
		extractors.add(new DataCollector(languages));

		for (Extractor extractor : extractors) {
			System.out.println(extractor.getName() + ", " + extractor.getSource() + " - " + extractor.getDescription());
			extractor.run();
		}
	}

	private void pipelineStep4() {

		List<Extractor> extractors = new ArrayList<Extractor>();

		// Collect relations from/to events
		extractors.add(new WikidataExtractionWithEventPages(languages, getAllEventPagesDataSet()));
		extractors.add(new DBpediaEventRelationsExtractor(languages, getAllEventPagesDataSet()));
		extractors.add(new CurrentEventsRelationsExtraction(languages, getAllEventPagesDataSet()));
		extractors.add(new YAGOEventRelationsExtractor(languages, getAllEventPagesDataSet()));

		for (Extractor extractor : extractors) {
			System.out.println(extractor.getName() + ", " + extractor.getSource() + " - " + extractor.getDescription());
			extractor.run();
		}

	}

	private void pipelineStep5() {

		List<Extractor> extractors = new ArrayList<Extractor>();
		getAllEventPagesDataSet();
		extractors.add(new TextualEventsExtractor(languages, getAllEventPagesDataSet()));
		extractors.add(new SubLocationsCollector(languages, getAllEventPagesDataSet())); //
		extractors.add(new LocationsIntegrator(languages));
		extractors.add(new TemporalRelationsCollector(languages, getAllEventPagesDataSet())); //
		extractors.add(new TimesIntegrator(languages));
		extractors.add(new WikipediaLinkCountsExtractor(languages, getAllEventPagesDataSet())); //
		extractors.add(new WikipediaLinkSetsExtractor(languages, getAllEventPagesDataSet())); //
		extractors.add(new LabelsAndDescriptionsExtractor(languages, getAllEventPagesDataSet())); //

		for (Extractor extractor : extractors) {
			System.out.println(extractor.getName() + ", " + extractor.getSource() + " - " + extractor.getDescription());
			extractor.run();
		}

		DataStoreWriter outputWriter = new DataStoreWriter();
		outputWriter.write();

		System.out.println("Done.");
	}

	private AllEventPagesDataSet getAllEventPagesDataSet() {
		if (allEventPagesDataSet == null) {
			this.allEventPagesDataSet = new AllEventPagesDataSet(languages);
			this.allEventPagesDataSet.init();
		}
		return this.allEventPagesDataSet;
	}

	private void initDataSets() {
		DataSets.getInstance().addDataSet(Language.DE, Source.DBPEDIA, "http://de.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.FR, Source.DBPEDIA, "http://fr.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.RU, Source.DBPEDIA, "http://ru.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.PT, Source.DBPEDIA, "http://pt.dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.EN, Source.DBPEDIA, "http://dbpedia.org/");
		DataSets.getInstance().addDataSet(Language.DE, Source.WIKIPEDIA, "https://dumps.wikimedia.org/dewiki/");
		DataSets.getInstance().addDataSet(Language.FR, Source.WIKIPEDIA, "https://dumps.wikimedia.org/frwiki/");
		DataSets.getInstance().addDataSet(Language.RU, Source.WIKIPEDIA, "https://dumps.wikimedia.org/ruwiki/");
		DataSets.getInstance().addDataSet(Language.PT, Source.WIKIPEDIA, "https://dumps.wikimedia.org/ptwiki/");
		DataSets.getInstance().addDataSet(Language.EN, Source.WIKIPEDIA, "https://dumps.wikimedia.org/enwiki/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.WIKIDATA,
				"https://dumps.wikimedia.org/wikidatawiki/entities/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.YAGO,
				"https://www.mpi-inf.mpg.de/de/departments/databases-and-information-systems/research/yago-naga/yago/downloads/");
		DataSets.getInstance().addDataSet(Language.EN, Source.WCE, "http://wikitimes.l3s.de/Resource.jsp");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_TIME, "http://eventkg.l3s.uni-hannover.de/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_TIME_2,
				"http://eventkg.l3s.uni-hannover.de/");
		DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_LOC, "http://eventkg.l3s.uni-hannover.de/");
		// DataSets.getInstance().addDataSetWithoutLanguage(Source.INTEGRATED_LOC_2,
		// "http://eventkg.l3s.uni-hannover.de/");
	}
}
