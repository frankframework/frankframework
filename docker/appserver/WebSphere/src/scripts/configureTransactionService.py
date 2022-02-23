print "Setting TransactionService settings"

transactionService = AdminConfig.list('TransactionService')

# Accept heuristic hazard to allow one phase resources (such as ActiveMQ) to participate in two phase commits
AdminConfig.modify(transactionService, [['acceptHeuristicHazard', 'true']])

AdminConfig.save()