package net.sf.okapi.acorn.common;

import java.io.File;
import java.util.Stack;

import net.sf.okapi.acorn.xom.Factory;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.IResource;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.filters.FilterConfigurationMapper;
import net.sf.okapi.common.filters.IFilter;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.filterwriter.IFilterWriter;
import net.sf.okapi.common.resource.Code;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.RawDocument;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.resource.TextFragment;
import net.sf.okapi.common.resource.TextFragment.TagType;

import org.oasisopen.xliff.om.v1.GetTarget;
import org.oasisopen.xliff.om.v1.ICTag;
import org.oasisopen.xliff.om.v1.IContent;
import org.oasisopen.xliff.om.v1.IDocument;
import org.oasisopen.xliff.om.v1.IFile;
import org.oasisopen.xliff.om.v1.IGroup;
import org.oasisopen.xliff.om.v1.IMTag;
import org.oasisopen.xliff.om.v1.ISegment;
import org.oasisopen.xliff.om.v1.IUnit;
import org.oasisopen.xliff.om.v1.IWithGroupOrUnit;

public class FilterBasedWriter implements IDocumentWriter {

	private final String filterConfigId;
	
	private IDocument doc;
	private IFilterWriter writer;
	private LocaleId srcLoc = LocaleId.ENGLISH;
	private LocaleId trgLoc = LocaleId.fromString("iu");
	
	public FilterBasedWriter (String filterConfigId) {
		this.filterConfigId = filterConfigId;
	}
	
	@Override
	public void merge (IDocument document,
		File originalFile)
	{
		this.doc = document;
		RawDocument rd = new RawDocument(originalFile.toURI(), "UTF-8", srcLoc, trgLoc, filterConfigId);
		IFilter filter = null;
		
		try {
			IFilterConfigurationMapper fcm = new FilterConfigurationMapper();
			fcm.addConfigurations("net.sf.okapi.filters.tmx.TmxFilter");
			fcm.addConfigurations("net.sf.okapi.filters.openxml.OpenXMLFilter");
			fcm.addConfigurations("net.sf.okapi.filters.html.HtmlFilter");
			filter = fcm.createFilter(filterConfigId);
			filter.open(rd);
			IFile file;
			Stack<IWithGroupOrUnit> parents = new Stack<>();
			
			while ( filter.hasNext() ) {
				Event event = filter.next();
				switch ( event.getEventType() ) {
				case START_DOCUMENT:
					writer = event.getStartDocument().getFilterWriter();
					writer.setOptions(LocaleId.fromString(doc.getTargetLanguage()), "UTF-8");
					StringBuilder outPath = new StringBuilder(originalFile.getAbsolutePath());
					// We assume the original file path has an extension
					outPath.insert(outPath.lastIndexOf("."), "_"+doc.getTargetLanguage());
					writer.setOutput(outPath.toString());
					break;
				case START_SUBDOCUMENT:
					file = doc.getFile(event.getStartSubDocument().getId());
					parents.push(file);
					break;
				case START_GROUP:
					if ( parents.isEmpty() ) {
						file = doc.getFile("f1");
						parents.push(file);
					}
					StartGroup sg = event.getStartGroup();
					IGroup group = parents.peek().getGroup(sg.getId());
					parents.peek().add(group);
					break;
				case TEXT_UNIT:
					if ( parents.isEmpty() ) {
						file = doc.getFile("f1");
						parents.push(file);
					}
					ITextUnit oriUnit = event.getTextUnit();
					IUnit docUnit = parents.peek().getUnit(oriUnit.getId());
					convert(oriUnit, docUnit);
					break;
				case END_GROUP:
					parents.pop();
					break;
				case END_SUBDOCUMENT:
					parents.pop();
					break;
				case END_BATCH_ITEM:
					if ( writer != null ) writer.close();
				default:
					break;
				}
				if ( writer != null ) {
					writer.handleEvent(event);
				}
			}
		}
		finally {
			if ( filter != null ) filter.close();
		}
		
	}
	
	private void convert (ITextUnit oriUnit,
		IUnit docUnit)
	{
		// Join all segments
		// (do this on a copy of the unit)
		IUnit mrgUnit = Factory.XOM.copyUnit(docUnit);
		mrgUnit.joinAll(true, true);

		// Convert the target
		ISegment trg = mrgUnit.getSegment(0);
		if ( trg.hasTarget() && !trg.getTarget().isEmpty() ) {
			IContent cont = trg.getTarget();
			if ( !oriUnit.hasTarget(trgLoc) ) {
				// Create an empty target
				oriUnit.createTarget(trgLoc, true, IResource.COPY_PROPERTIES);
				// Port the translated content in that new target
				TextFragment tf = oriUnit.getTarget(trgLoc).getFirstContent();
				fillContent(tf, cont);
			}
		}
	}

	private void fillContent (TextFragment dest,
		IContent original)
	{
		for ( Object obj : original ) {
			if ( obj instanceof ICTag ) {
				ICTag ctag = (ICTag)obj;
				Code code;
				switch ( ctag.getTagType() ) {
				case OPENING:
					code = dest.append(TagType.OPENING, ctag.getType(), ctag.getData(),
						Integer.parseInt(ctag.getId()));
					break;
				case CLOSING:
					code = dest.append(TagType.CLOSING, ctag.getType(), ctag.getData(),
						Integer.parseInt(ctag.getId()));
					break;
				case STANDALONE:
				default:
					code = dest.append(TagType.PLACEHOLDER, ctag.getType(), ctag.getData(),
						Integer.parseInt(ctag.getId()));
					break;
				}
				if ( ctag.getSubFlows() != null ) code.setReferenceFlag(true);
			}
			else if ( obj instanceof IMTag ) {
				//TODO
			}
			else { // String
				dest.append((String)obj);
			}
		}
	}
	
//	private void copyCode (Code oriCode,
//		ICTag dstCTag)
//	{
//		dstCTag.setCanCopy(oriCode.isCloneable());
//		dstCTag.setCanDelete(oriCode.isDeleteable());
//		//dstCTag.setCanOverlap(oriCTag.getCanOverlap());
//		//dstCTag.setCanReorder(convCanReorder(oriCTag.getCanReorder()));
//		//dstCTag.setCopyOf(oriCTag.getCopyOf());
//		//dstCTag.setDataDir(convDir(oriCTag.getDataDir()));
//		//dstCTag.setDir(convDir(oriCTag.getDir()));
//		dstCTag.setDisp(oriCode.getDisplayText());
//		//dstCTag.setEquiv(oriCTag.getEquiv());
//		//dstCTag.setSubFlows(oriCTag.getSubFlows());
//		//dstCTag.setSubType(oriCTag.getSubType());
//		//??? dstCTag.setType(oriCode.getType());
//	}

}