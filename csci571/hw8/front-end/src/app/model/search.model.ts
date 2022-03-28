export interface Search {
    description: string;
    displaySymbol: string;
    symbol: string;
    type: string;
}

export interface SearchResp {
    count: number;
    result: Search[];
}