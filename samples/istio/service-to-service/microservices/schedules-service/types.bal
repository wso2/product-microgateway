public type ScheduleItem record {
    # Id of the schedule item.
    string entryId?;
    # Train starting time.
    string startTime?;
    # Train destination arrival time.
    string endTime?;
    # Train starting station.
    string 'from?;
    # Train destination station.
    string to?;
    # Id of the train.
    string trainId?;
};

public type ScheduleItemInfo record {
    # Id of the schedule item.
    string entryId?;
    # Train starting time.
    string startTime?;
    # Train destination arrival time.
    string endTime?;
    # Train starting station.
    string 'from?;
    # Train destination station.
    string to?;
    # Id of the train.
    string trainId?;
    # Facilities provided in the train.
    string facilities?;
    # Image URL of the train.
    string imageURL?;
};

public type Train record {
    # Id of the schedule item.
    string trainId?;
    # Number of train carriages.
    int numberOfCarriage?;
    # Image URL of the train.
    string imageURL?;
    # Engine model.
    string engineModel?;
    # Facilities provided in the train.
    string facilities?;
};
