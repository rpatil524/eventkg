package de.l3s.eventkg.integration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.l3s.eventkg.integration.model.DateGranularity;
import de.l3s.eventkg.integration.model.Entity;
import de.l3s.eventkg.integration.model.Event;
import de.l3s.eventkg.meta.Language;
import de.l3s.eventkg.meta.Source;

public class TextualEvent {

	private Language language;

	private Event mainEvent;

	private Source source;

	private String id;

	private String text;

	private Set<Entity> relatedEntities;
	private Set<Event> relatedEvents = new HashSet<Event>();

	private String startDate;

	private String endDate;

	private Set<TextualEvent> candidateSimilarEvents = new HashSet<TextualEvent>();

	private String wikipediaPage;

	private DateGranularity granularity;

	private String englishWCECategory;

	private Map<Language, Set<String>> otherCategories = new HashMap<Language, Set<String>>();

	private Set<String> sources = new HashSet<String>();

	public TextualEvent(Language language, Source source, String id, String text, Set<Entity> relatedEntities,
			String startDate, String endDate, String wikipediaPage, DateGranularity granularity) {
		super();
		this.language = language;
		this.source = source;
		this.id = id;
		this.text = text;
		this.relatedEntities = relatedEntities;
		this.startDate = startDate;
		this.endDate = endDate;
		this.wikipediaPage = wikipediaPage;
		this.granularity = granularity;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Set<Entity> getRelatedEntities() {
		return relatedEntities;
	}

	public void setRelatedEntities(Set<Entity> relatedEntities) {
		this.relatedEntities = relatedEntities;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public Set<TextualEvent> getCandidateSimilarEvents() {
		return candidateSimilarEvents;
	}

	public void setCandidateSimilarEvents(Set<TextualEvent> candidateSimilarEvents) {
		this.candidateSimilarEvents = candidateSimilarEvents;
	}

	public void addCandidateSimilarEvent(TextualEvent candidateSimilarEvent) {
		this.candidateSimilarEvents.add(candidateSimilarEvent);
	}

	public Set<Event> getRelatedEvents() {
		return relatedEvents;
	}

	public void setRelatedEvents(Set<Event> relatedEvents) {
		this.relatedEvents = relatedEvents;
	}

	public void addRelatedEvent(Event relatedEvent) {
		this.relatedEvents.add(relatedEvent);
	}

	public Event getMainEvent() {
		return mainEvent;
	}

	public void setMainEvent(Event mainEvent) {
		this.mainEvent = mainEvent;
	}

	public String getWikipediaPage() {
		return wikipediaPage;
	}

	public void setWikipediaPage(String wikipediaPage) {
		this.wikipediaPage = wikipediaPage;
	}

	public DateGranularity getGranularity() {
		return granularity;
	}

	public void setGranularity(DateGranularity granularity) {
		this.granularity = granularity;
	}

	public String getEnglishWCECategory() {
		return englishWCECategory;
	}

	public void setEnglishWCECategory(String category) {
		this.englishWCECategory = category;
	}

	public Map<Language, Set<String>> getOtherCategories() {
		return otherCategories;
	}

	public void setOtherCategories(Map<Language, Set<String>> otherCategories) {
		this.otherCategories = otherCategories;
	}

	public Set<String> getSources() {
		return sources;
	}

	public void setSources(Set<String> sources) {
		this.sources = sources;
	}

}
