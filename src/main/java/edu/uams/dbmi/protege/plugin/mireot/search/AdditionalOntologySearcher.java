package edu.uams.dbmi.protege.plugin.mireot.search;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.search.EntitySearcher;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import edu.uams.dbmi.protege.plugin.mireot.search.result.ClassSearchResult;
import edu.uams.dbmi.protege.plugin.mireot.search.result.ObjectPropertySearchResult;
import edu.uams.dbmi.protege.plugin.mireot.search.result.SearchResult;
import edu.uams.dbmi.protege.plugin.mireot.search.result.table.ResultTableCellRenderer;


import java.awt.Component;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * Searcher which loads an ontology and searches it 
 * by label, comment, and definition for matching classes or object properties
 * 
 * @author Cheng Chen, Josh Hanna
 */
public class AdditionalOntologySearcher {

	private String query;
	private String oldQuery;
	private String ontUrl;

	private String oldOntUrl;

	private OWLOntologyManager man;
	private OWLOntology ontology;
	private OWLDataFactory factory;

	private boolean searchByLabel = true;
	private boolean searchByComment = false;
	private boolean searchByDefinition = false;
	private boolean searchByURI = true;

	private boolean searchClasses = true;
	private boolean searchObjectProperties = false;

	private boolean optionsChanged;

	private ArrayList<SearchResult> results = new ArrayList<SearchResult>();
	private File ontFile;
	private File oldOntFile;

	/*
	 * Constructors
	 */

	/**
	 * Builds searcher using ontology located at @param url and prepares @param query 
	 * 
	 * @author Cheng Chen, Josh Hanna
	 * @param String query
	 * @param String url
	 */
	public AdditionalOntologySearcher(String query, String url) {
		setQuery(query);
		setUrl(url);
	}

	public AdditionalOntologySearcher() {
	}

	/*
	 * Accessors
	 */



	public void setQuery(String query) {
		this.query = query;
	}


	public boolean searchByLabelFlag() {
		return searchByLabel;
	}

	public void setSearchByLabelFlag(boolean searchByLabel) {
		this.searchByLabel = searchByLabel;
		optionsChanged = true;
	}

	public boolean searchByCommentFlag() {
		return searchByComment;
	}

	public void setSearchByCommentFlag(boolean searchByComment) {
		this.searchByComment = searchByComment;
		optionsChanged = true;
	}

	public boolean searchByDefinitionFlag() {
		return searchByDefinition;
	}

	public void setSearchByDefinitionFlag(boolean searchByDefinition) {
		this.searchByDefinition = searchByDefinition;
		optionsChanged = true;
	}
	
	public boolean searchByURIFlag() {
		return searchByURI;
	}

	public void setSearchByURIFlag(boolean searchByURI) {
		this.searchByURI = searchByURI;
		optionsChanged = true;
	}	

	public boolean searchClassesFlag() {
		return searchClasses;
	}

	public void setSearchClassesFlag(boolean searchClasses) {
		this.searchClasses = searchClasses;
		optionsChanged = true;
	}

	public boolean searchObjectPropertiesFlag() {
		return searchObjectProperties;
	}

	public void setSearchObjectPropertiesFlag(boolean searchObjectProperties) {
		this.searchObjectProperties = searchObjectProperties;
		optionsChanged = true;
	}

	public void setUrl(String ontUrl) {
		this.ontUrl = ontUrl;
	}


	public OWLOntologyManager getAdditionalManager() {
		return this.man;
	}

	public OWLDataFactory getAdditionalFactory() {
		return this.factory;
	}

	public OWLOntology getAdditonalOntology() {
		return this.ontology;
	}

	public ArrayList<SearchResult> getResults(){
		synchronized(results){
			return this.results;
		}
	}

	public void setFile(File ontFile){
		this.ontFile = ontFile;
	}

	/**
	 * Loads ontology using URI passed in earlier
	 * 
	 * @author Josh Hanna
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 * @throws Exception 
	 */
	private void loadOntology() throws IOException, OWLOntologyCreationException {


		if(this.ontUrl != null && this.oldOntUrl != null){

			if(this.ontUrl.equals(oldOntUrl)){
				//same ontology, no need to reload
				return;
			} else {
				//keeping track of old url
				this.oldOntUrl = this.ontUrl;
			}

		} else {
			//keeping track of old url
			this.oldOntUrl = this.ontUrl;
		} 

		if(this.ontFile != null && this.oldOntFile != null){
			if(this.ontFile.equals(oldOntFile)){

				return;
			} else {
				this.oldOntFile = this.ontFile;
			}
		} else {
			this.oldOntFile = ontFile;
		}

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();

		try {
			System.out.println("Loading ontology...");

			if(ontUrl == null && ontFile == null){
				throw new IOException("Either ontology file or URL must exist.");
			} else if(ontUrl != null){ 
				
				this.ontology = man.loadOntology(IRI.create(ontUrl));
			} else if(ontFile != null){
				
				this.ontology = man.loadOntologyFromOntologyDocument(ontFile);
				
			}
			
		} catch (OWLOntologyCreationException e) {
			JOptionPane.showMessageDialog(null, "Could not load ontology: " + e.getMessage());
			e.printStackTrace();

			throw e;
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Could not load ontology: " + e.getMessage());
			e.printStackTrace();
			throw e;
		}

		this.factory = man.getOWLDataFactory();

		this.man = man;
		System.out.println("Loading finished.");





	}

	/*
	 * Search Routines
	 */

	/**
	 * Loads and then searches ontology based on options
	 * 
	 * @author Josh Hanna
	 * @return ArrayList of SearchResult that represents matching Object Properties and/or Classes (depending on options)
	 * @throws IOException 
	 * @throws OWLOntologyCreationException 
	 * @throws Exception 
	 */
	public void search() throws OWLOntologyCreationException, IOException {

		results.clear();
		synchronized(results){

			loadOntology();


			if(this.searchClassesFlag()){
				results.addAll(this.searchByClass());
			}
            
			if(this.searchObjectPropertiesFlag()){
				results.addAll(this.searchByObjectProperty());
			}

			
			results = this.removeDuplicates(results);
		}

	}

	private ArrayList<SearchResult> removeDuplicates(ArrayList<SearchResult> results) {
		ArrayList<SearchResult> searchList = new ArrayList<SearchResult>(results);
		ArrayList<SearchResult> processed = new ArrayList<SearchResult>();
		for(SearchResult result1 : searchList){

			for(SearchResult result2 : searchList){
				if(!processed.contains(result2)){
					if(!result1.equals(result2)){


						if(result1.getIRI().toString().equalsIgnoreCase(result2.getIRI().toString())){

							results.remove(result2);
						}
					}
				}
			}
			processed.add(result1);
		}
		return results;

	}

	/**
	 * 
	 * @author Josh Hanna
	 * @return ArrayList<SearchResult> representing all matching Object Properties
	 */
	public ArrayList<SearchResult> searchByObjectProperty(){
		OWLDataFactory df = getAdditionalFactory();
		String lowerCaseQuery = query.toLowerCase();

		ArrayList<SearchResult> resultList = new ArrayList<SearchResult>();

		OWLAnnotationProperty label = df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());		
		OWLAnnotationProperty comment = df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());		
		OWLAnnotationProperty definition = df.getOWLAnnotationProperty(IRI
				.create("http://purl.obolibrary.org/obo/IAO_0000115"));

		if (this.searchByLabelFlag()) {

			resultList.addAll(searchObjectProperties(lowerCaseQuery, label));

		}

		if (this.searchByDefinitionFlag()) {

			resultList.addAll(searchObjectProperties(lowerCaseQuery, definition));

		}

		if (this.searchByCommentFlag()) {

			resultList.addAll(searchObjectProperties(lowerCaseQuery, comment));

		}
        
        if(this.searchByURIFlag()) {
			
			resultList.addAll(this.searchObjectProperties(lowerCaseQuery));
		}

		return resultList;
	}
    
    

	/**
	 * 
	 * @author Josh Hanna
	 * @return ArrayList<SearchResult> representing all matching Classes
	 */
	public ArrayList<SearchResult> searchByClass() {

		OWLDataFactory df = getAdditionalFactory();

		String lowercaseQuery = query.toLowerCase();

		ArrayList<SearchResult> resultList = new ArrayList<SearchResult>();


		OWLAnnotationProperty label = df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());


		OWLAnnotationProperty comment = df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_COMMENT.getIRI());
		
		OWLAnnotationProperty definition = df.getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/IAO_0000115"));
		


		if (this.searchByLabelFlag()) {

			resultList.addAll(this.searchClasses(lowercaseQuery, label));
			

		}

		if (this.searchByCommentFlag()) {
			

			resultList.addAll(this.searchClasses(lowercaseQuery, comment));

		}


		if (this.searchByDefinitionFlag()) {
			

			resultList.addAll(this.searchClasses(lowercaseQuery, definition));

		}
		
		if(this.searchByURIFlag()) {
			
			resultList.addAll(this.searchClasses(lowercaseQuery));
		}
		


		return resultList;
	}

	/**
	 * 
	 * @author Josh Hanna
	 * @param query
	 * @param annotationProperty
	 * @return ArrayList<SearchResults> of any object properties that contain an annotation of type annotationProperty that matches query
	 */
	private ArrayList<SearchResult> searchObjectProperties(String query, OWLAnnotationProperty annotationProperty) {

		//list of ontology and imports
		ArrayList<OWLOntology> ontologies = new ArrayList<OWLOntology>();

		ontologies.add(ontology);
		
		Set<OWLOntology> imports = getAdditionalManager().getImports(ontology);

		for(OWLOntology ont : imports){
			ontologies.add(ont);
		}

		ArrayList<SearchResult> resultList = new ArrayList<SearchResult>();

		for(OWLOntology ont : ontologies){
			for (OWLObjectProperty objectProperty : ont.getObjectPropertiesInSignature()) {

				// Get the annotations on the object property that use the annotation property
				for (OWLAnnotation annotation : EntitySearcher.getAnnotations(objectProperty.getIRI(), ont, annotationProperty)) {

					if (annotation.getValue() instanceof OWLLiteral) {
						OWLLiteral val2 = (OWLLiteral) annotation.getValue();

						if (val2.getLiteral().toString().toLowerCase().contains(query)) {
							IRI labelIri = objectProperty.getIRI();

							String labelName = getLabel(objectProperty, ont);
							String matchType = annotationProperty.toString();

							String matchContext = val2.getLiteral();

							SearchResult resultItem = new ObjectPropertySearchResult(labelIri, labelName, matchType, matchContext, objectProperty, ontology);
							resultList.add(resultItem);
						}
					}
				}
			}
		}
		return resultList;
	}
    
    private ArrayList<SearchResult> searchObjectProperties(String query) {

		//list of ontology and imports
		ArrayList<OWLOntology> ontologies = new ArrayList<OWLOntology>();

		ontologies.add(ontology);
		
		Set<OWLOntology> imports = getAdditionalManager().getImports(ontology);

		for(OWLOntology ont : imports){
			ontologies.add(ont);
		}

		ArrayList<SearchResult> resultList = new ArrayList<SearchResult>();

		for(OWLOntology ont : ontologies){
			for (OWLObjectProperty objectProperty : ont.getObjectPropertiesInSignature()) {

				String uri = objectProperty.toStringID().toLowerCase();

				if(uri.equals(query)){	
					
					IRI labelIri = objectProperty.getIRI();

					String labelName = getLabel(objectProperty, ont);
					String matchType = "URI";
					String matchContext = "NA";

					//String matchContext = val2.getLiteral();

					SearchResult resultItem = new ObjectPropertySearchResult(labelIri, labelName, matchType, matchContext, objectProperty, ontology);

					resultList.add(resultItem);
				}
			}
		}
		return resultList;
	}
	

	/**
	 * 
	 * @author Josh Hanna
	 * @param query
	 * @param annotationProperty
	 * @return ArrayList<SearchResults> of any classes that contain an annotation of type annotationProperty that matches query
	 */
	private ArrayList<SearchResult> searchClasses(String query, OWLAnnotationProperty annotationProperty){
		

		//list of ontology and imports
		ArrayList<OWLOntology> ontologies = new ArrayList<OWLOntology>();
		ontologies.add(ontology);
		
		Set<OWLOntology> imports = getAdditionalManager().getImports(ontology);
		

		for(OWLOntology ont : imports) {
			ontologies.add(ont);
		}
		

		ArrayList<SearchResult> resultList = new ArrayList<SearchResult>();

		for(OWLOntology ont : ontologies){
			
			for (OWLClass cls : ont.getClassesInSignature()) {


				// Get the annotations on the class that use the label property
				for (OWLAnnotation annotation : EntitySearcher.getAnnotations(cls.getIRI(), ont, annotationProperty)) {


					if (annotation.getValue() instanceof OWLLiteral) {
						OWLLiteral val2 = (OWLLiteral) annotation.getValue();

						if (val2.getLiteral().toString().toLowerCase().contains(query)) {
							IRI labelIri = cls.getIRI();

							String labelName = getLabel(cls, ont);
							String matchType = annotationProperty.toString();

							String matchContext = val2.getLiteral();

							ClassSearchResult LabelResultList = new ClassSearchResult(labelIri, labelName, matchType, matchContext, cls, ontology);

							resultList.add(LabelResultList);
						}
					}
				}
			}
		}

		return resultList;
	}
	
	private ArrayList<SearchResult> searchClasses(String query){
		

		//list of ontology and imports
		ArrayList<OWLOntology> ontologies = new ArrayList<OWLOntology>();
		ontologies.add(ontology);
		
		Set<OWLOntology> imports = getAdditionalManager().getImports(ontology);
		

		for(OWLOntology ont : imports) {
			ontologies.add(ont);
		}
		

		ArrayList<SearchResult> resultList = new ArrayList<SearchResult>();

		for(OWLOntology ont : ontologies){
			
			for (OWLClass cls : ont.getClassesInSignature()) {

				String uri = cls.toStringID().toLowerCase();
				if(uri.equals(query)){	
					
					IRI labelIri = cls.getIRI();

					String labelName = getLabel(cls, ont);
					String matchType = "URI";
					String matchContext = "NA";

					//String matchContext = val2.getLiteral();

					ClassSearchResult LabelResultList = new ClassSearchResult(labelIri, labelName, matchType, matchContext, cls, ontology);

					resultList.add(LabelResultList);
				}
			}
		}

		return resultList;
	}


	/**
	 * 
	 * @author Josh Hanna
	 * @param entity
	 * @param ontology
	 * @return A String that represents the first english Label on an OWL Entity
	 */
	String getLabel(OWLEntity entity, OWLOntology ontology) {

		OWLDataFactory df = getAdditionalFactory();

		String label = "";
		OWLAnnotationProperty labelProperty = df
				.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI());

		// Get the annotations on the class that use the label property
		for (OWLAnnotation annotation : EntitySearcher.getAnnotations(entity.getIRI(), ontology, labelProperty)) {
			if (annotation.getValue() instanceof OWLLiteral) {
				OWLLiteral val = (OWLLiteral) annotation.getValue();
				if (val.hasLang("en") || !val.hasLang()) {
					label = val.getLiteral().toString();
				} else {
					String ClassString = entity.getIRI().toString();
					label = ClassString.substring(ClassString
							.indexOf("#") + 1);
				}
			}
		}
		return label;
	}

	public boolean isSameOntology(){
		if(ontUrl != null && oldOntUrl != null && ontUrl.equals(oldOntUrl)){
			return true;
		}
		if(ontFile != null && oldOntFile != null && ontFile.equals(oldOntFile)){
			return true;
		}
		return false;
	}

	public synchronized void buildResultTable(final String[] columnNames, final DefaultTableModel tableModel,
			final JTable resultTable) {


		//if it's the same search as last time, no need to do anything
		if((!optionsChanged) && (this.query.equalsIgnoreCase(this.oldQuery) && this.isSameOntology())){
			return;
		} else {

			oldQuery = query;
			optionsChanged = false;

			Thread t = new Thread() {
				public void run(){


					this.clearTableModel(tableModel);


					setStatus("Starting search", tableModel);

					try {
						search();
					} catch (Exception e) {
						setStatus("Error", tableModel);
						return;
					} 
					

					ArrayList<SearchResult> resultList = getResults();


					if (resultList.size() == 0) {
						setStatus("No results", tableModel);

					} else {
						this.updateTable(resultList, resultTable, tableModel, columnNames);
					}
				}

				private void setStatus(String str, final DefaultTableModel tableModel) {

					final Object[][] status = new Object[1][1];
					status[0][0] = str;

					final Object[] header = new Object[1];
					header[0] = "Status";

					EventQueue.invokeLater(new Runnable(){

						@Override
						public void run() {
							tableModel.setDataVector(status, header);
						}

					});

				}

				private void updateTable(final ArrayList<SearchResult> resultList,
						final JTable resultTable, final DefaultTableModel tableModel, final String[] columnNames) {

					EventQueue.invokeLater(new Runnable(){


						@Override
						public void run() {

							Object[][] resultData = new Object[resultList.size()][4];

							//custom renderer for tooltips
							ResultTableCellRenderer renderer = new ResultTableCellRenderer();

							for (int i = 0; i < resultList.size(); i++) {

								resultData[i][0] = resultList.get(i).getType();
								resultData[i][1] = resultList.get(i).getName();
								resultData[i][2] = resultList.get(i).getOWLEntity().getIRI();
								resultData[i][3] = resultList.get(i).getMatchType();

								tableModel.setDataVector(resultData, columnNames);

								renderer.setTooltip(i, resultList.get(i).getMatchContext());

							}

							//setting custom renderer on matchType column for tooltip
							TableColumn col = resultTable.getColumnModel().getColumn(3);
							col.setCellRenderer(renderer);
							packColumns(resultTable, 5);


						}
					});
				}




				private void clearTableModel(final DefaultTableModel tableModel) {
					try {

						EventQueue.invokeAndWait(new Runnable(){

							public void run(){

								while (tableModel.getRowCount() > 0) {
									tableModel.removeRow(tableModel.getRowCount() - 1);
								}

							}
						});

					} catch (InterruptedException e) {
						e.printStackTrace();

					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}

				}



				private void packColumns(JTable table, int margin) {
					for (int c=0; c<table.getColumnCount(); c++) {
						packColumn(table, c, 2);
					} 
				}

				public void packColumn(JTable table, int vColIndex, int margin) {
					DefaultTableColumnModel colModel = (DefaultTableColumnModel)table.getColumnModel();
					TableColumn col = colModel.getColumn(vColIndex);
					int width = 0;

					// Get width of column header
					TableCellRenderer renderer = col.getHeaderRenderer();
					if (renderer == null) {
						renderer = table.getTableHeader().getDefaultRenderer();
					}
					Component comp = renderer.getTableCellRendererComponent(
							table, col.getHeaderValue(), false, false, 0, 0);
					width = comp.getPreferredSize().width;

					// Get maximum width of column data
					for (int r=0; r<table.getRowCount(); r++) {
						renderer = table.getCellRenderer(r, vColIndex);
						comp = renderer.getTableCellRendererComponent(
								table, table.getValueAt(r, vColIndex), false, false, r, vColIndex);
						width = Math.max(width, comp.getPreferredSize().width);
					}

					// Add margin
					width += 2*margin;

					// Set the width
					col.setPreferredWidth(width);
				}	

			};

			t.start();

		}
	}

}
