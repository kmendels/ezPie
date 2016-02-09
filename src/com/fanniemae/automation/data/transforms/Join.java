package com.fanniemae.automation.data.transforms;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.fanniemae.automation.SessionManager;
import com.fanniemae.automation.common.DataStream;
import com.fanniemae.automation.common.StringUtilities;
import com.fanniemae.automation.common.XmlUtilities;
import com.fanniemae.automation.data.DataEngine;

public class Join extends DataTransform {

	protected int _columnCount;

	protected String[] _leftColumnNames;
	protected String[] _rightColumnNames;

	protected JoinType _joinType;

	protected String _joinText;
	
	protected Node _rightDataSource;

	protected enum JoinType {
		INNERJOIN, LEFTOUTERJOIN, RIGHTOUTERJOIN, OUTERJOIN, UNION, CROSSJOIN
	}

	public Join(SessionManager session, Element operation) {
		super(session, operation, false);

		String leftColumnList = _Session.getAttribute(operation, "LeftColumns");
		String rightColumnList = _Session.getAttribute(operation, "RightColumns");

		if (StringUtilities.isNullOrEmpty(leftColumnList)) {
			throw new RuntimeException("Join requires at least one column name in LeftColumns.");
		} else if (StringUtilities.isNullOrEmpty(rightColumnList)) {
			throw new RuntimeException("Join requires at least one column name in RightColumns.");
		}

		_leftColumnNames = StringUtilities.split(leftColumnList);
		_rightColumnNames = StringUtilities.split(rightColumnList);

		if (_leftColumnNames.length != _rightColumnNames.length) {
			throw new RuntimeException(String.format("The number of columns for the left and right data sets must be equal (%d left columns != %d right columns)", _leftColumnNames.length, _rightColumnNames.length));
		}

		String joinType = _Session.getAttribute(operation, "JoinType");
		if (StringUtilities.isNullOrEmpty(joinType)) {
			joinType = "Inner";
		}

		_joinType = setJoinType(joinType);

		StringBuilder sb = new StringBuilder(_joinText);
		for (int i = 0; i < _leftColumnNames.length; i++) {
			if (i > 0)
				sb.append(",");
			sb.append(String.format(" left.%s = right.%s", _leftColumnNames[i], _rightColumnNames[i]));
		}
		_TransformInfo.append(sb.toString());
		
		_rightDataSource = XmlUtilities.selectSingleNode(operation,"DataSource");
		if (_rightDataSource == null) {
			throw new RuntimeException("Missing right side data source.  Joins require a nested DataSource for the right side.");
		}
	}

	@Override
	public boolean isTableLevel() {
		return true;
	}

	@Override
	public Object[] processDataRow(Object[] dataRow) {
		throw new RuntimeException("Join requires access to the entire data set.  It cannot be combined with other data transformations.");
	}

	@Override
	public DataStream processDataStream(DataStream inputStream, int memoryLimit) {
		DataStream outputStream = null;
		// Get the right side data (create new instance of data engine)
		DataEngine de = new DataEngine(_Session);
		DataStream rightDataStream = de.getData((Element)_rightDataSource);
		// Index the right and left dataStreams on the join columns
		

		// Join the data and write the final file.

		return outputStream;
	}

	protected JoinType setJoinType(String join) {
		switch (join.toLowerCase()) {
		case "inner":
			_joinText = "Inner Join";
			return JoinType.INNERJOIN;
		case "left":
		case "left outer":
			_joinText = "Left Outer";
			return JoinType.LEFTOUTERJOIN;
		case "right":
		case "right outer":
			_joinText = "Right Outer";
			return JoinType.RIGHTOUTERJOIN;
		case "full outer":
		case "outer":
			_joinText = "Full Outer";
			return JoinType.OUTERJOIN;
		case "union":
			_joinText = "Union";
			return JoinType.UNION;
		case "cross":
		case "cross join":
			_joinText = "Cross Join";
			return JoinType.CROSSJOIN;
		default:
			throw new RuntimeException(String.format("%s join type is not currently supported.", join));
		}
	}
}